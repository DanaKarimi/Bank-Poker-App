import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { UserBadge, AvatarPickerModal, PokerAvatar } from './AvatarSystem';
import {
  X,
  UserCheck,
  Shield,
  KeyRound,
  AlertCircle,
  CheckCircle2,
  LogOut,
  Bell,
  User,
  Sliders,
  Check
} from 'lucide-react';
import { getNotificationSettings, updateNotificationSettings } from '../api';

export const ProfileModal = ({ onClose }) => {
  const { user, updateProfile, activateAccount, logout } = useAuth();

  const [activeTab, setActiveTab] = useState('profile'); // 'profile' | 'notifications'

  const [displayName, setDisplayName] = useState(user?.display_name || user?.username || '');
  const [avatarId, setAvatarId] = useState(user?.avatar_id || 'avatar_1');
  const [showAvatarPicker, setShowAvatarPicker] = useState(false);

  // Guest activation state
  const [showActivate, setShowActivate] = useState(false);
  const [actUsername, setActUsername] = useState('');
  const [actPassword, setActPassword] = useState('');

  // Notification settings state
  const [notifSettings, setNotifSettings] = useState({
    table_created: true,
    member_joined: true,
    request_to_me: true,
    settlement: true
  });
  const [loadingNotif, setLoadingNotif] = useState(false);
  const [savingNotif, setSavingNotif] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isGuest = user?.is_guest || user?.role === 'GUEST' || user?.username?.startsWith('guest_');

  useEffect(() => {
    const loadSettings = async () => {
      setLoadingNotif(true);
      try {
        const res = await getNotificationSettings();
        if (res.data?.settings) {
          const s = res.data.settings;
          setNotifSettings({
            table_created: Boolean(s.table_created ?? true),
            member_joined: Boolean(s.member_joined ?? true),
            request_to_me: Boolean(s.request_to_me ?? true),
            settlement: Boolean(s.settlement ?? true)
          });
        }
      } catch (err) {
        console.error('Failed to load notification settings:', err);
      } finally {
        setLoadingNotif(false);
      }
    };
    loadSettings();
  }, []);

  const handleToggleSetting = async (key) => {
    const updated = {
      ...notifSettings,
      [key]: !notifSettings[key]
    };
    setNotifSettings(updated);
    setSavingNotif(true);
    try {
      await updateNotificationSettings(updated);
      setSuccess('Notification preferences saved!');
      setTimeout(() => setSuccess(''), 2500);
    } catch (err) {
      console.error('Failed to save settings:', err);
      setError('Failed to update notification settings.');
      // Revert on failure
      setNotifSettings(notifSettings);
      setTimeout(() => setError(''), 3000);
    } finally {
      setSavingNotif(false);
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!displayName.trim()) {
      setError('Display name cannot be empty.');
      return;
    }
    setError('');
    setSuccess('');
    setIsSubmitting(true);
    try {
      await updateProfile(displayName.trim(), avatarId);
      setSuccess('Profile updated successfully!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.error || 'Failed to update profile.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleActivateAccount = async (e) => {
    e.preventDefault();
    if (!actUsername.trim() || !actPassword.trim()) {
      setError('Both username and password are required for activation.');
      return;
    }
    setError('');
    setSuccess('');
    setIsSubmitting(true);
    try {
      await activateAccount(actUsername.trim(), actPassword);
      setSuccess('Account permanently activated! You now have full account credentials.');
      setShowActivate(false);
      setTimeout(() => setSuccess(''), 4000);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.error || 'Failed to activate account.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-md p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header & Tabs */}
        <div className="text-center pb-3 border-b border-gold-accent/20 mb-4">
          <h2 className="text-xl font-black text-gold-accent tracking-wider uppercase mb-3">
            Settings & Profile
          </h2>
          <div className="flex gap-2 justify-center">
            <button
              type="button"
              onClick={() => setActiveTab('profile')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                activeTab === 'profile'
                  ? 'bg-gold-accent text-black shadow'
                  : 'bg-felt-dark text-cream-text/70 hover:text-cream-text border border-gold-accent/30'
              }`}
            >
              <User className="w-3.5 h-3.5" />
              <span>Profile</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('notifications')}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                activeTab === 'notifications'
                  ? 'bg-gold-accent text-black shadow'
                  : 'bg-felt-dark text-cream-text/70 hover:text-cream-text border border-gold-accent/30'
              }`}
            >
              <Bell className="w-3.5 h-3.5" />
              <span>Notifications</span>
            </button>
          </div>
        </div>

        {/* Feedback Alerts */}
        {error && (
          <div className="mb-4 p-3 bg-red-950/80 border border-red-500 rounded-xl flex items-center gap-2 text-red-200 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
        )}
        {success && (
          <div className="mb-4 p-3 bg-green-950/80 border border-green-500 rounded-xl flex items-center gap-2 text-green-200 text-xs">
            <CheckCircle2 className="w-4 h-4 shrink-0 text-green-400" />
            <span>{success}</span>
          </div>
        )}

        {/* TAB 1: PROFILE */}
        {activeTab === 'profile' && (
          <div>
            {/* Current Profile Preview */}
            <div className="flex flex-col items-center gap-3 p-4 bg-felt-dark/70 rounded-xl border border-gold-accent/30 mb-5">
              <div
                onClick={() => setShowAvatarPicker(true)}
                className="cursor-pointer group relative"
                title="Click to change avatar"
              >
                <PokerAvatar avatarId={avatarId} name={displayName} size={68} />
                <div className="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 flex items-center justify-center transition text-[10px] text-gold-accent font-bold">
                  Change
                </div>
              </div>

              <UserBadge
                displayName={displayName}
                username={user?.username}
                avatarId={avatarId}
                role={user?.role}
                isGuest={isGuest}
                size={0}
                className="justify-center"
              />

              <button
                type="button"
                onClick={() => setShowAvatarPicker(true)}
                className="text-xs text-gold-accent hover:underline font-semibold cursor-pointer"
              >
                Change Avatar (24 bundled styles)
              </button>
            </div>

            {/* Edit Profile Form */}
            <form onSubmit={handleUpdateProfile} className="space-y-4 mb-5">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gold-light mb-1">
                  Display Name
                </label>
                <input
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Your table display name"
                  className="w-full px-3.5 py-2 bg-felt-dark border border-gold-accent/50 rounded-xl text-cream-text placeholder-cream-text/40 text-sm focus:outline-none focus:border-gold-accent"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-2.5 bg-gradient-to-r from-gold-accent via-yellow-500 to-gold-accent text-black font-bold uppercase tracking-wider text-xs rounded-xl shadow hover:opacity-95 active:scale-98 transition disabled:opacity-50 cursor-pointer"
              >
                {isSubmitting ? 'Saving...' : 'Save Profile Changes'}
              </button>
            </form>

            {/* Guest Activation Section */}
            {isGuest && (
              <div className="p-4 bg-gold-accent/10 border border-gold-accent/40 rounded-xl mb-5 text-left">
                <div className="flex items-center gap-2 text-gold-accent font-bold text-xs mb-1">
                  <KeyRound className="w-4 h-4" />
                  <span>Temporary Guest Account</span>
                </div>
                <p className="text-[11px] text-cream-text/75 mb-3">
                  Upgrade to a full permanent account to keep all your stats, groups, and bank records forever.
                </p>

                {!showActivate ? (
                  <button
                    type="button"
                    onClick={() => setShowActivate(true)}
                    className="w-full py-2 bg-felt-dark hover:bg-felt-dark/80 text-gold-accent font-bold text-xs rounded-lg border border-gold-accent/50 cursor-pointer transition"
                  >
                    Activate Permanent Account
                  </button>
                ) : (
                  <form onSubmit={handleActivateAccount} className="space-y-2.5 pt-2 border-t border-gold-accent/20">
                    <input
                      type="text"
                      value={actUsername}
                      onChange={(e) => setActUsername(e.target.value)}
                      placeholder="Choose unique username"
                      className="w-full px-3 py-1.5 bg-felt-dark border border-gold-accent/50 rounded-lg text-cream-text text-xs placeholder-cream-text/40 focus:outline-none focus:border-gold-accent"
                      required
                    />
                    <input
                      type="password"
                      value={actPassword}
                      onChange={(e) => setActPassword(e.target.value)}
                      placeholder="Choose password"
                      className="w-full px-3 py-1.5 bg-felt-dark border border-gold-accent/50 rounded-lg text-cream-text text-xs placeholder-cream-text/40 focus:outline-none focus:border-gold-accent"
                      required
                    />
                    <button
                      type="submit"
                      disabled={isSubmitting}
                      className="w-full py-2 bg-gold-accent text-black font-bold text-xs rounded-lg hover:bg-gold-light transition cursor-pointer"
                    >
                      {isSubmitting ? 'Activating...' : 'Confirm Permanent Activation'}
                    </button>
                  </form>
                )}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: NOTIFICATIONS */}
        {activeTab === 'notifications' && (
          <div className="space-y-3 mb-5">
            <p className="text-xs text-cream-text/70 mb-3">
              Choose which in-app and push notification alerts you want to receive:
            </p>

            {[
              {
                key: 'table_created',
                title: 'New Tables',
                desc: 'Alert when a table is created in your groups'
              },
              {
                key: 'member_joined',
                title: 'Member Joined',
                desc: 'Alert when someone joins your group'
              },
              {
                key: 'request_to_me',
                title: 'Action Requests',
                desc: 'Alert when buy-in or cashout requests need review'
              },
              {
                key: 'settlement',
                title: 'Settlement Plans',
                desc: 'Alert when group debts settlement plans are calculated'
              }
            ].map(({ key, title, desc }) => (
              <div
                key={key}
                onClick={() => handleToggleSetting(key)}
                className="flex items-center justify-between p-3.5 bg-felt-dark/70 hover:bg-felt-dark border border-gold-accent/30 rounded-xl cursor-pointer transition select-none"
              >
                <div className="pr-3">
                  <div className="text-xs font-bold text-cream-text">{title}</div>
                  <div className="text-[11px] text-cream-text/60 leading-snug">{desc}</div>
                </div>

                <div
                  className={`w-11 h-6 rounded-full transition-colors flex items-center p-1 shrink-0 ${
                    notifSettings[key] ? 'bg-gold-accent' : 'bg-gray-700'
                  }`}
                >
                  <div
                    className={`w-4 h-4 rounded-full bg-felt-card shadow-md transform transition-transform ${
                      notifSettings[key] ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Footer Actions */}
        <div className="pt-4 border-t border-gold-accent/20 flex items-center justify-between">
          <button
            type="button"
            onClick={() => {
              logout();
              onClose();
            }}
            className="flex items-center gap-1.5 text-xs text-red-400 hover:text-red-300 font-semibold cursor-pointer transition"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>

          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 bg-felt-dark hover:bg-felt-dark/80 text-cream-text font-bold text-xs rounded-lg border border-gold-accent/30 cursor-pointer transition"
          >
            Done
          </button>
        </div>
      </div>

      {showAvatarPicker && (
        <AvatarPickerModal
          selectedAvatarId={avatarId}
          onSelect={(id) => setAvatarId(id)}
          onClose={() => setShowAvatarPicker(false)}
        />
      )}
    </div>
  );
};

export default ProfileModal;
