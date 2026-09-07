import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  CheckCheck,
  Layers,
  UserPlus,
  DollarSign,
  CheckCircle2,
  RefreshCw,
  X,
  Clock
} from 'lucide-react';
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead
} from '../api';

const formatRelativeTime = (timestamp) => {
  if (!timestamp) return '';
  const now = Date.now();
  const time = typeof timestamp === 'number' ? timestamp : new Date(timestamp).getTime();
  if (isNaN(time)) return '';
  const diffSec = Math.floor((now - time) / 1000);
  if (diffSec < 60) return 'just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}d ago`;
  return new Date(time).toLocaleDateString([], { month: 'short', day: 'numeric' });
};

const getNotificationDetails = (item) => {
  const p = item.payload || {};
  switch (item.type) {
    case 'table_created':
      return {
        title: 'New Table Created',
        desc: p.tableName ? `Table "${p.tableName}" was opened` : (p.message || 'A new poker table was started'),
        Icon: Layers,
        badgeColor: 'text-emerald-400 bg-emerald-950/60 border-emerald-500/40',
      };
    case 'member_joined':
      return {
        title: 'Member Joined',
        desc: p.username ? `@${p.username} joined the group` : (p.message || 'A new player joined'),
        Icon: UserPlus,
        badgeColor: 'text-blue-400 bg-blue-950/60 border-blue-500/40',
      };
    case 'request_to_me':
      return {
        title: 'Request For You',
        desc: p.message || `New ${p.type || 'action'} request pending your review`,
        Icon: DollarSign,
        badgeColor: 'text-gold-accent bg-yellow-950/60 border-gold-accent/40',
      };
    case 'settlement':
      return {
        title: 'Settlement Generated',
        desc: p.message || 'A settlement plan was generated or updated',
        Icon: CheckCircle2,
        badgeColor: 'text-purple-400 bg-purple-950/60 border-purple-500/40',
      };
    default:
      return {
        title: p.title || 'Notification',
        desc: p.message || 'You have an update',
        Icon: Bell,
        badgeColor: 'text-cream-text bg-felt-card border-gold-accent/30',
      };
  }
};

export const NotificationsDropdown = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('all'); // 'all' | 'unread'
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [isMarkingAll, setIsMarkingAll] = useState(false);

  const dropdownRef = useRef(null);

  const fetchNotificationList = async (background = false) => {
    if (!background) setLoading(true);
    try {
      const res = await getNotifications();
      const list = res.data?.notifications || [];
      const unread = res.data?.unreadCount ?? list.filter(n => !n.read).length;
      setNotifications(list);
      setUnreadCount(unread);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      if (!background) setLoading(false);
    }
  };

  // Initial fetch and 30s background polling
  useEffect(() => {
    fetchNotificationList(true);
    const interval = setInterval(() => {
      fetchNotificationList(true);
    }, 30000);
    return () => clearInterval(interval);
  }, []);

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const toggleDropdown = () => {
    if (!isOpen) {
      fetchNotificationList(false);
    }
    setIsOpen(!isOpen);
  };

  const handleMarkAllAsRead = async () => {
    if (unreadCount === 0 || isMarkingAll) return;
    setIsMarkingAll(true);
    try {
      await markAllNotificationsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    } finally {
      setIsMarkingAll(false);
    }
  };

  const handleNotificationClick = async (item) => {
    if (!item.read) {
      try {
        await markNotificationRead(item.id);
        setNotifications(prev =>
          prev.map(n => (n.id === item.id ? { ...n, read: true } : n))
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
      } catch (err) {
        console.error('Failed to mark single notification as read:', err);
      }
    }

    setIsOpen(false);

    const p = item.payload || {};
    const targetTableId = p.tableId || p.table_id;
    const targetGroupId = p.groupId || p.group_id;

    if (targetTableId) {
      if (targetGroupId) {
        navigate(`/group/${targetGroupId}/table/${targetTableId}`);
      } else {
        navigate(`/table/${targetTableId}`);
      }
    } else if (targetGroupId) {
      navigate(`/group/${targetGroupId}`);
    }
  };

  const filteredNotifications = notifications.filter(n => {
    if (activeTab === 'unread') return !n.read;
    return true;
  });

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Button */}
      <button
        type="button"
        onClick={toggleDropdown}
        className="relative p-2 rounded-xl bg-felt-card hover:bg-felt-card/80 border border-gold-accent/40 text-gold-accent hover:text-yellow-300 transition select-none shadow cursor-pointer focus:outline-none focus:ring-2 focus:ring-gold-accent/50"
        title="Notifications"
        aria-label="Open notifications"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1.5 -right-1.5 bg-red-600 text-white font-black text-[10px] min-w-[19px] h-[19px] px-1 rounded-full flex items-center justify-center border-2 border-felt-dark shadow-md animate-pulse">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Panel */}
      {isOpen && (
        <div className="absolute right-0 mt-2.5 w-80 sm:w-96 max-w-[calc(100vw-2rem)] bg-felt-dark border border-gold-accent/50 rounded-2xl shadow-2xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
          {/* Header */}
          <div className="px-4 py-3 bg-felt-card border-b border-gold-accent/30 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-extrabold text-sm text-gold-accent uppercase tracking-wider">
                Notifications
              </span>
              {unreadCount > 0 && (
                <span className="bg-red-900/70 border border-red-500/50 text-red-200 text-[10px] font-bold px-2 py-0.5 rounded-full">
                  {unreadCount} new
                </span>
              )}
            </div>

            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllAsRead}
                  disabled={isMarkingAll}
                  className="text-[11px] font-semibold text-gold-accent hover:text-yellow-200 flex items-center gap-1 transition cursor-pointer disabled:opacity-50"
                  title="Mark all as read"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  <span>Mark all read</span>
                </button>
              )}
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="text-cream-text/50 hover:text-cream-text p-1 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Filter Tabs */}
          <div className="flex border-b border-gold-accent/20 bg-felt-dark/60">
            <button
              type="button"
              onClick={() => setActiveTab('all')}
              className={`flex-1 py-2 text-xs font-bold transition border-b-2 cursor-pointer ${
                activeTab === 'all'
                  ? 'border-gold-accent text-gold-accent bg-gold-accent/10'
                  : 'border-transparent text-cream-text/60 hover:text-cream-text'
              }`}
            >
              All ({notifications.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('unread')}
              className={`flex-1 py-2 text-xs font-bold transition border-b-2 cursor-pointer ${
                activeTab === 'unread'
                  ? 'border-gold-accent text-gold-accent bg-gold-accent/10'
                  : 'border-transparent text-cream-text/60 hover:text-cream-text'
              }`}
            >
              Unread ({unreadCount})
            </button>
          </div>

          {/* Notifications List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-gold-accent/15">
            {loading && notifications.length === 0 ? (
              <div className="py-10 text-center space-y-2">
                <RefreshCw className="w-5 h-5 text-gold-accent animate-spin mx-auto" />
                <p className="text-xs text-cream-text/60">Loading notifications...</p>
              </div>
            ) : filteredNotifications.length === 0 ? (
              <div className="py-12 text-center px-4 space-y-2">
                <Bell className="w-8 h-8 text-cream-text/20 mx-auto" />
                <p className="text-xs font-semibold text-cream-text/70">
                  {activeTab === 'unread' ? 'No unread notifications' : 'No notifications yet'}
                </p>
                <p className="text-[11px] text-cream-text/40">
                  You will be notified when tables are created or requests arrive.
                </p>
              </div>
            ) : (
              filteredNotifications.map((item) => {
                const { title, desc, Icon, badgeColor } = getNotificationDetails(item);
                return (
                  <div
                    key={item.id}
                    onClick={() => handleNotificationClick(item)}
                    className={`p-3.5 flex items-start gap-3 transition cursor-pointer hover:bg-felt-card/70 ${
                      !item.read ? 'bg-gold-accent/[0.07]' : ''
                    }`}
                  >
                    <div className={`p-2 rounded-xl border shrink-0 ${badgeColor}`}>
                      <Icon className="w-4 h-4" />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1 mb-0.5">
                        <span className={`text-xs font-bold truncate ${!item.read ? 'text-gold-accent' : 'text-cream-text'}`}>
                          {title}
                        </span>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="text-[10px] text-cream-text/50 flex items-center gap-0.5">
                            <Clock className="w-2.5 h-2.5" />
                            {formatRelativeTime(item.createdAt)}
                          </span>
                          {!item.read && (
                            <span className="w-2 h-2 rounded-full bg-gold-accent shadow-sm shrink-0" />
                          )}
                        </div>
                      </div>

                      <p className="text-[11px] text-cream-text/80 line-clamp-2 leading-relaxed">
                        {desc}
                      </p>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationsDropdown;
