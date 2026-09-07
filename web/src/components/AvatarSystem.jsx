import React from 'react';
import { X, Check } from 'lucide-react';

export const BUNDLED_AVATARS = [
  { id: 'avatar_1', name: 'Ace of Spades', symbol: '♠', primaryColor: '#E2E8F0', bgColor: '#0F172A' },
  { id: 'avatar_2', name: 'King of Hearts', symbol: '♥', primaryColor: '#EF4444', bgColor: '#450A0A' },
  { id: 'avatar_3', name: 'Queen Diamonds', symbol: '♦', primaryColor: '#38BDF8', bgColor: '#082F49' },
  { id: 'avatar_4', name: 'Jack of Clubs', symbol: '♣', primaryColor: '#10B981', bgColor: '#064E3B' },
  { id: 'avatar_5', name: 'Wild Joker', symbol: '🃏', primaryColor: '#A855F7', bgColor: '#3B0764' },
  { id: 'avatar_6', name: 'Card Shark', symbol: '🦈', primaryColor: '#06B6D4', bgColor: '#164E63' },
  { id: 'avatar_7', name: 'High Roller', symbol: '🐂', primaryColor: '#F97316', bgColor: '#7C2D12' },
  { id: 'avatar_8', name: 'Clever Fox', symbol: '🦊', primaryColor: '#FB923C', bgColor: '#431407' },
  { id: 'avatar_9', name: 'Eagle Eye', symbol: '🦅', primaryColor: '#FACC15', bgColor: '#422006' },
  { id: 'avatar_10', name: 'Royal Tiger', symbol: '🐯', primaryColor: '#F59E0B', bgColor: '#451A03' },
  { id: 'avatar_11', name: 'Golden Lion', symbol: '🦁', primaryColor: '#EAB308', bgColor: '#3B2D05' },
  { id: 'avatar_12', name: 'Mythic Dragon', symbol: '🐉', primaryColor: '#EC4899', bgColor: '#500724' },
  { id: 'avatar_13', name: 'Grizzly Bear', symbol: '🐻', primaryColor: '#D97706', bgColor: '#382006' },
  { id: 'avatar_14', name: 'Shadow Wolf', symbol: '🐺', primaryColor: '#94A3B8', bgColor: '#1E293B' },
  { id: 'avatar_15', name: 'Night Hawk', symbol: '🦅', primaryColor: '#818CF8', bgColor: '#1E1B4B' },
  { id: 'avatar_16', name: 'The Crown', symbol: '👑', primaryColor: '#FDE047', bgColor: '#422006' },
  { id: 'avatar_17', name: 'Lucky Dice', symbol: '🎲', primaryColor: '#F43F5E', bgColor: '#4C0519' },
  { id: 'avatar_18', name: 'All-In Star', symbol: '⭐', primaryColor: '#FDE047', bgColor: '#451A03' },
  { id: 'avatar_19', name: 'Blazing Flame', symbol: '🔥', primaryColor: '#FB7185', bgColor: '#7F1D1D' },
  { id: 'avatar_20', name: 'Iron Shield', symbol: '🛡️', primaryColor: '#64748B', bgColor: '#0F172A' },
  { id: 'avatar_21', name: 'Mind Wizard', symbol: '🧙', primaryColor: '#C084FC', bgColor: '#3B0764' },
  { id: 'avatar_22', name: 'Royal Knight', symbol: '⚔️', primaryColor: '#60A5FA', bgColor: '#172554' },
  { id: 'avatar_23', name: 'Lucky Clover', symbol: '🍀', primaryColor: '#4ADE80', bgColor: '#052E16' },
  { id: 'avatar_24', name: 'The Vault', symbol: '🏛️', primaryColor: '#2DD4BF', bgColor: '#042F2E' }
];

export const getAvatarInfo = (avatarId, fallbackName = '') => {
  if (avatarId) {
    const found = BUNDLED_AVATARS.find((a) => a.id.toLowerCase() === avatarId.toLowerCase());
    if (found) return found;
  }
  if (fallbackName) {
    let hash = 0;
    for (let i = 0; i < fallbackName.length; i++) {
      hash = (hash << 5) - hash + fallbackName.charCodeAt(i);
      hash |= 0;
    }
    const idx = Math.abs(hash) % BUNDLED_AVATARS.length;
    return BUNDLED_AVATARS[idx];
  }
  return BUNDLED_AVATARS[0];
};

export const PokerAvatar = ({ avatarId, name = '', size = 42, className = '' }) => {
  const avatar = getAvatarInfo(avatarId, name);
  const fontSize = Math.round(size * 0.45);

  return (
    <div
      style={{
        width: `${size}px`,
        height: `${size}px`,
        backgroundColor: avatar.bgColor,
        color: avatar.primaryColor,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)'
      }}
      className={`relative inline-flex items-center justify-center rounded-full border-2 border-gold-accent shrink-0 select-none ${className}`}
      title={avatar.name}
    >
      {/* Dashed Casino Rim */}
      <div className="absolute inset-0.5 rounded-full border border-dashed border-white/20 pointer-events-none" />
      <span style={{ fontSize: `${fontSize}px` }} className="font-bold leading-none">
        {avatar.symbol}
      </span>
    </div>
  );
};

export const UserBadge = ({
  displayName,
  username,
  avatarId,
  role,
  isGuest,
  size = 40,
  showUsername = true,
  className = '',
  onClick
}) => {
  const nameToDisplay = displayName || username || 'Player';
  const isGuestAccount = isGuest || role === 'GUEST' || username?.startsWith('guest_');

  return (
    <div
      onClick={onClick}
      className={`inline-flex items-center gap-2.5 ${onClick ? 'cursor-pointer hover:opacity-90' : ''} ${className}`}
    >
      <PokerAvatar avatarId={avatarId} name={nameToDisplay} size={size} />

      <div className="flex flex-col justify-center min-w-0 text-left">
        <div className="flex items-center gap-1.5">
          <span className="font-bold text-cream-text text-sm truncate leading-tight">
            {nameToDisplay}
          </span>
          {role === 'SUPER_ADMIN' && (
            <span className="px-1.5 py-0.2 text-[9px] font-black uppercase tracking-wider bg-gold-accent text-black rounded">
              SUPER
            </span>
          )}
          {role === 'ADMIN' && (
            <span className="px-1.5 py-0.2 text-[9px] font-black uppercase tracking-wider bg-gold-accent/20 border border-gold-accent text-gold-accent rounded">
              HOST
            </span>
          )}
        </div>

        {showUsername && !isGuestAccount && username && (
          <span className="text-[11px] text-cream-text/60 truncate font-mono leading-tight">
            @{username}
          </span>
        )}
        {showUsername && isGuestAccount && (
          <span className="text-[10px] text-gold-accent/80 font-semibold leading-tight">
            Guest
          </span>
        )}
      </div>
    </div>
  );
};

export const AvatarPickerModal = ({ selectedAvatarId, onSelect, onClose }) => {
  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-felt-card border-2 border-gold-accent rounded-2xl w-full max-w-lg p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
        <div className="flex items-center justify-between pb-4 border-b border-gold-accent/30 mb-4">
          <h3 className="text-xl font-bold text-gold-accent">Choose Your Avatar</h3>
          <button
            onClick={onClose}
            className="text-cream-text/60 hover:text-cream-text p-1 rounded-lg transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="grid grid-cols-4 sm:grid-cols-6 gap-3 max-h-[380px] overflow-y-auto pr-1">
          {BUNDLED_AVATARS.map((avatar) => {
            const isSelected = selectedAvatarId?.toLowerCase() === avatar.id.toLowerCase();
            return (
              <button
                key={avatar.id}
                type="button"
                onClick={() => {
                  onSelect(avatar.id);
                  onClose();
                }}
                className={`p-2 rounded-xl flex flex-col items-center gap-1.5 transition cursor-pointer border ${
                  isSelected
                    ? 'bg-gold-accent/20 border-gold-accent shadow-lg shadow-gold-accent/10'
                    : 'bg-felt-dark/60 border-gold-accent/20 hover:border-gold-accent/60'
                }`}
              >
                <div className="relative">
                  <PokerAvatar avatarId={avatar.id} size={48} />
                  {isSelected && (
                    <div className="absolute -top-1 -right-1 bg-gold-accent text-black rounded-full p-0.5 shadow">
                      <Check className="w-3 h-3 stroke-[3]" />
                    </div>
                  )}
                </div>
                <span
                  className={`text-[10px] font-medium truncate w-full text-center ${
                    isSelected ? 'text-gold-accent font-bold' : 'text-cream-text/80'
                  }`}
                >
                  {avatar.name}
                </span>
              </button>
            );
          })}
        </div>

        <div className="mt-5 pt-4 border-t border-gold-accent/20 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 bg-felt-dark hover:bg-felt-dark/80 text-cream-text font-bold rounded-xl border border-gold-accent/40 text-sm cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
