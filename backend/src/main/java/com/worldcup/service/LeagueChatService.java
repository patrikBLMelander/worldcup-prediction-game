package com.worldcup.service;

import com.worldcup.dto.ChatMessageDTO;
import com.worldcup.dto.LeagueUnreadDTO;
import com.worldcup.entity.League;
import com.worldcup.entity.LeagueChatRead;
import com.worldcup.entity.LeagueMembership;
import com.worldcup.entity.LeagueMessage;
import com.worldcup.entity.User;
import com.worldcup.exception.LeagueNotFoundException;
import com.worldcup.exception.UnauthorizedException;
import com.worldcup.repository.LeagueChatReadRepository;
import com.worldcup.repository.LeagueMembershipRepository;
import com.worldcup.repository.LeagueMessageRepository;
import com.worldcup.repository.LeagueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * League chat: members of a league can read and post messages in that league,
 * with per-user unread tracking for the chat badge.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LeagueChatService {

    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final LeagueMessageRepository messageRepository;
    private final LeagueChatReadRepository readRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getMessages(Long leagueId, User user) {
        League league = requireMembership(leagueId, user);
        List<LeagueMessage> recent = messageRepository.findTop200ByLeagueOrderByCreatedAtDesc(league);
        Collections.reverse(recent); // oldest first for display
        return recent.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ChatMessageDTO postMessage(Long leagueId, User user, String content) {
        League league = requireMembership(leagueId, user);
        LeagueMessage message = new LeagueMessage();
        message.setLeague(league);
        message.setUser(user);
        message.setContent(content.trim());
        LeagueMessage saved = messageRepository.save(message);

        // Posting implicitly catches the author up to their own message.
        markReadInternal(league, user);

        return toDTO(saved);
    }

    public void markRead(Long leagueId, User user) {
        League league = requireMembership(leagueId, user);
        markReadInternal(league, user);
    }

    @Transactional(readOnly = true)
    public List<LeagueUnreadDTO> unreadSummary(User user) {
        List<LeagueMembership> memberships = membershipRepository.findByUser(user);
        List<LeagueUnreadDTO> out = new ArrayList<>();

        for (LeagueMembership membership : memberships) {
            League league = membership.getLeague();
            if (league == null || Boolean.TRUE.equals(league.getHidden())) {
                continue;
            }
            // Unread = messages since the user last read (or since they joined), by someone else.
            LocalDateTime baseline = readRepository.findByLeagueAndUser(league, user)
                    .map(LeagueChatRead::getLastReadAt)
                    .orElse(membership.getJoinedAt());
            long unread = messageRepository.countByLeagueAndCreatedAtAfterAndUserNot(league, baseline, user);
            out.add(new LeagueUnreadDTO(league.getId(), league.getName(), unread));
        }
        return out;
    }

    private void markReadInternal(League league, User user) {
        LeagueChatRead read = readRepository.findByLeagueAndUser(league, user)
                .orElseGet(() -> {
                    LeagueChatRead fresh = new LeagueChatRead();
                    fresh.setLeague(league);
                    fresh.setUser(user);
                    return fresh;
                });
        read.setLastReadAt(LocalDateTime.now());
        readRepository.save(read);
    }

    private League requireMembership(Long leagueId, User user) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException(leagueId));
        membershipRepository.findByLeagueAndUser(league, user)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this league"));
        return league;
    }

    private ChatMessageDTO toDTO(LeagueMessage m) {
        return new ChatMessageDTO(
                m.getId(),
                m.getLeague().getId(),
                m.getUser().getId(),
                m.getUser().getScreenName(),
                m.getContent(),
                m.getCreatedAt()
        );
    }
}
