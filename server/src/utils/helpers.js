/**
 * Generate a random 6-character alphanumeric invite code (uppercase letters + numbers)
 * @returns {string} 6-character invite code (e.g. '7A9K2X')
 */
function generateInviteCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let code = '';
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

module.exports = {
  generateInviteCode
};
