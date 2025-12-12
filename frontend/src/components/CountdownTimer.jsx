import { useState, useEffect, useRef } from 'react';

const CountdownTimer = ({ matchDate, status, matchId, onExpired }) => {
  const [timeLeft, setTimeLeft] = useState(null);
  const [expiredSeconds, setExpiredSeconds] = useState(0);
  const hasTriggeredExpired = useRef(false);

  useEffect(() => {
    // Only run timer if status is SCHEDULED or if match time has passed (to catch status updates)
    if (!matchDate) {
      setTimeLeft(null);
      setExpiredSeconds(0);
      hasTriggeredExpired.current = false;
      return;
    }

    const updateTimer = () => {
      const now = new Date();
      const matchTime = new Date(matchDate);
      const diff = matchTime - now;

      if (diff <= 0) {
        setTimeLeft({ expired: true });
        setExpiredSeconds(prev => prev + 1);
        // Trigger status update to LIVE when countdown expires (only once initially)
        if (!hasTriggeredExpired.current && onExpired && matchId) {
          hasTriggeredExpired.current = true;
          onExpired(matchId);
        }
        return;
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      setTimeLeft({ days, hours, minutes, seconds, expired: false });
      setExpiredSeconds(0);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);

    return () => clearInterval(interval);
  }, [matchDate, status, matchId, onExpired]);

  // Poll more frequently when expired to catch status change
  useEffect(() => {
    if (timeLeft?.expired && onExpired && matchId) {
      // Refresh every 1 second when expired to catch the status change as quickly as possible
      const refreshInterval = setInterval(() => {
        onExpired(matchId);
      }, 1000);
      return () => clearInterval(refreshInterval);
    }
  }, [timeLeft?.expired, onExpired, matchId]);

  // Show timer if status is SCHEDULED, or if match time has passed (to show "Match starting..." while waiting for status update)
  if (!timeLeft) {
    return null;
  }

  // If status has changed to LIVE, don't show the timer anymore
  if (status === 'LIVE' || status === 'FINISHED') {
    return null;
  }

  if (timeLeft.expired) {
    // Show a more informative message with elapsed time
    return (
      <span className="countdown-timer expired">
        Match starting{expiredSeconds > 0 ? ` (${expiredSeconds}s)` : ''}...
      </span>
    );
  }

  const { days, hours, minutes, seconds } = timeLeft;

  // Format based on time remaining
  if (days > 0) {
    return (
      <span className="countdown-timer">
        {days}d {hours}h {minutes}m
      </span>
    );
  } else if (hours > 0) {
    return (
      <span className="countdown-timer">
        {hours}h {minutes}m {seconds}s
      </span>
    );
  } else {
    return (
      <span className="countdown-timer urgent">
        {minutes}m {seconds}s
      </span>
    );
  }
};

export default CountdownTimer;

