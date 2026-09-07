import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { UserBadge, AvatarPickerModal, PokerAvatar } from './AvatarSystem';
import { X, UserCheck, Shield, KeyRound, AlertCircle, CheckCircle2, LogOut } from 'lucide-react';

export const ProfileModal = ({ onClose }) => {
  const { user, updateProfile, activateAccount, logout } = useAuth();

  const [displayName, setDisplayName] = useState(user?.display_name || user?.username || '');
  const [avatarId, setAvatarId] = useState(user?.avatar_id || 'avatar_1');
  const [showAvatarPicker, setShowAvatarPicker] = useState(false);

  // Guest activation state
  const [showActivate, setShowActivate] = useState(false);
  const [actUsername, setActUsername] = useState('');
  const [actPassword, setActPassword] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isGuest = user?.is_guest || user?.role === 'GUEST' || user?.username?.startsWith('guest_');

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

        {/* Modal Title */}
        <div className="text-center pb-4 border-b border-gold-accent/20 mb-5">
          <h2 className="text-xl font-black text-gold-accent tracking-wider uppercase">
            Player Profile
          </h2>
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
