/**
 * 密码加密工具
 * 使用 Base64 + 简单异或加密，确保密码在传输过程中不以明文形式出现
 * 后端可解密还原为明文，再进行 BCrypt 比对
 */

// 简单的加密密钥（前后端保持一致，建议从配置读取）
const ENCRYPT_KEY = 'xyswzl-sys-encryption-key-2026'

/**
 * 将字符串转为 Base64（支持中文）
 */
function stringToBase64(str) {
  const encoder = new TextEncoder()
  const bytes = encoder.encode(str)
  let binary = ''
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

/**
 * 使用密钥对字符串进行异或加密，结果用 Base64 编码
 * 注意：这是简单对称加密，仅用于传输脱敏，并非高强度加密
 */
export function encryptPassword(password) {
  if (!password) return ''

  // 将密码和密钥逐字节异或
  let result = ''
  for (let i = 0; i < password.length; i++) {
    const charCode = password.charCodeAt(i) ^ ENCRYPT_KEY.charCodeAt(i % ENCRYPT_KEY.length)
    result += String.fromCharCode(charCode)
  }

  // 加上前缀标识，方便后端识别是否已加密
  return 'ENC:' + stringToBase64(result)
}

/**
 * 对表单中的密码字段进行加密
 * 接收一个对象，返回新对象，其中 password 和 confirmPassword 字段被加密
 */
export function encryptFormData(formData) {
  const result = { ...formData }
  if (result.password !== undefined) {
    result.password = encryptPassword(result.password)
  }
  if (result.confirmPassword !== undefined) {
    result.confirmPassword = encryptPassword(result.confirmPassword)
  }
  return result
}

export default encryptPassword
