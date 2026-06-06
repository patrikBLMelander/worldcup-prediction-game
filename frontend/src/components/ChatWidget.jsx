import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';
import './ChatWidget.css';

const formatTime = (value) => {
  if (!value) return '';
  const iso = value.endsWith('Z') ? value : `${value}Z`;
  const d = new Date(iso);
  if (isNaN(d)) return '';
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const ChatWidget = () => {
  const { isAuthenticated, user } = useAuth();

  const [open, setOpen] = useState(false);
  const [view, setView] = useState('list'); // 'list' | 'chat'
  const [leagues, setLeagues] = useState([]);
  const [activeLeague, setActiveLeague] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [unread, setUnread] = useState([]); // [{leagueId, leagueName, unreadCount}]

  const messagesEndRef = useRef(null);

  const totalUnread = unread.reduce((sum, u) => sum + (u.unreadCount || 0), 0);
  const unreadFor = (leagueId) =>
    unread.find((u) => u.leagueId === leagueId)?.unreadCount || 0;

  const fetchUnread = useCallback(async () => {
    try {
      const res = await apiClient.get('/chat/unread');
      setUnread(res.data || []);
    } catch {
      /* transient; ignore */
    }
  }, []);

  // Poll unread summary while logged in (drives the badge even when closed)
  useEffect(() => {
    if (!isAuthenticated) return undefined;
    fetchUnread();
    const t = setInterval(fetchUnread, 20000);
    return () => clearInterval(t);
  }, [isAuthenticated, fetchUnread]);

  const openLeague = useCallback((league) => {
    setActiveLeague({ id: league.id, name: league.name });
    setView('chat');
  }, []);

  const openWidget = useCallback(async () => {
    setOpen(true);
    try {
      const res = await apiClient.get('/leagues/mine');
      const mine = res.data || [];
      setLeagues(mine);
      if (mine.length === 1) {
        openLeague(mine[0]);
      } else {
        setView('list');
      }
    } catch {
      setLeagues([]);
      setView('list');
    }
  }, [openLeague]);

  // Load + poll messages for the active league while the chat view is open
  useEffect(() => {
    if (!open || view !== 'chat' || !activeLeague) return undefined;
    let cancelled = false;

    const load = async () => {
      try {
        const res = await apiClient.get(`/chat/leagues/${activeLeague.id}/messages`);
        if (cancelled) return;
        setMessages(res.data || []);
        // Viewing the chat marks it read, then refresh the badge.
        await apiClient.post(`/chat/leagues/${activeLeague.id}/read`).catch(() => {});
        fetchUnread();
      } catch {
        /* transient; ignore */
      }
    };

    load();
    const t = setInterval(load, 4000);
    return () => {
      cancelled = true;
      clearInterval(t);
    };
  }, [open, view, activeLeague, fetchUnread]);

  // Keep the message list pinned to the latest
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async (e) => {
    e?.preventDefault();
    const text = input.trim();
    if (!text || !activeLeague || sending) return;
    setSending(true);
    try {
      await apiClient.post(`/chat/leagues/${activeLeague.id}/messages`, { content: text });
      setInput('');
      const res = await apiClient.get(`/chat/leagues/${activeLeague.id}/messages`);
      setMessages(res.data || []);
      fetchUnread();
    } catch (err) {
      console.error('Failed to send message:', err);
    } finally {
      setSending(false);
    }
  };

  const backToList = () => {
    setView('list');
    setActiveLeague(null);
    setMessages([]);
    fetchUnread();
  };

  const close = () => setOpen(false);

  if (!isAuthenticated) return null;

  const showBack = view === 'chat' && leagues.length > 1;

  return (
    <div className="chat-widget">
      {open && (
        <div className="chat-panel" role="dialog" aria-label="League chat">
          <div className="chat-panel-header">
            {showBack ? (
              <button className="chat-icon-btn" onClick={backToList} aria-label="Back to leagues">←</button>
            ) : (
              <span className="chat-panel-icon">💬</span>
            )}
            <span className="chat-panel-title">
              {view === 'chat' && activeLeague ? activeLeague.name : 'League chat'}
            </span>
            <button className="chat-icon-btn" onClick={close} aria-label="Close chat">×</button>
          </div>

          {view === 'list' ? (
            <div className="chat-league-list">
              {leagues.length === 0 ? (
                <p className="chat-empty">Join a league to start chatting.</p>
              ) : (
                leagues.map((l) => {
                  const n = unreadFor(l.id);
                  return (
                    <button key={l.id} className="chat-league-row" onClick={() => openLeague(l)}>
                      <span className="chat-league-name">{l.name}</span>
                      {n > 0 && <span className="chat-badge chat-badge-inline">{n > 99 ? '99+' : n}</span>}
                    </button>
                  );
                })
              )}
            </div>
          ) : (
            <>
              <div className="chat-messages">
                {messages.length === 0 ? (
                  <p className="chat-empty">No messages yet. Say hello! 👋</p>
                ) : (
                  messages.map((m) => {
                    const mine = m.userId === user?.id;
                    return (
                      <div key={m.id} className={`chat-msg ${mine ? 'mine' : 'theirs'}`}>
                        {!mine && <span className="chat-msg-author">{m.screenName || 'Player'}</span>}
                        <span className="chat-msg-bubble">{m.content}</span>
                        <span className="chat-msg-time">{formatTime(m.createdAt)}</span>
                      </div>
                    );
                  })
                )}
                <div ref={messagesEndRef} />
              </div>
              <form className="chat-input-row" onSubmit={send}>
                <input
                  type="text"
                  className="chat-input"
                  placeholder="Message…"
                  value={input}
                  maxLength={2000}
                  onChange={(e) => setInput(e.target.value)}
                />
                <button type="submit" className="chat-send-btn" disabled={sending || !input.trim()}>
                  Send
                </button>
              </form>
            </>
          )}
        </div>
      )}

      <button
        className="chat-fab"
        onClick={() => (open ? close() : openWidget())}
        aria-label={open ? 'Close chat' : 'Open league chat'}
      >
        {open ? '×' : '💬'}
        {!open && totalUnread > 0 && (
          <span className="chat-badge chat-badge-fab">{totalUnread > 99 ? '99+' : totalUnread}</span>
        )}
      </button>
    </div>
  );
};

export default ChatWidget;
