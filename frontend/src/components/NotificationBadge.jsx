import './NotificationBadge.css';

const NotificationBadge = ({ count }) => {
  if (!count || count === 0) {
    return null;
  }

  const displayCount = count > 99 ? '99+' : count;

  return (
    <span className="notification-badge" aria-label={`${count} unread notifications`}>
      {displayCount}
    </span>
  );
};

export default NotificationBadge;


