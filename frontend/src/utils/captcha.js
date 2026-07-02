/**
 * 前端验证码生成工具
 * 使用 Canvas 生成随机验证码图片，无需后端接口
 */

// 生成指定长度的随机字符串
function generateRandomCode(length = 4) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < length; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

// 生成随机颜色
function randomColor(min, max) {
  const r = Math.floor(Math.random() * (max - min) + min)
  const g = Math.floor(Math.random() * (max - min) + min)
  const b = Math.floor(Math.random() * (max - min) + min)
  return `rgb(${r},${g},${b})`
}

/**
 * 生成验证码图片
 * @param {number} width - 图片宽度
 * @param {number} height - 图片高度
 * @returns {{ code: string, dataUrl: string }} - 验证码文本和 base64 图片
 */
export function generateCaptcha(width = 120, height = 40) {
  const code = generateRandomCode(4)
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')

  // 绘制背景
  ctx.fillStyle = randomColor(180, 240)
  ctx.fillRect(0, 0, width, height)

  // 绘制干扰线
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = randomColor(100, 200)
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  // 绘制验证码字符
  for (let i = 0; i < code.length; i++) {
    ctx.font = `${Math.floor(height * 0.6)}px Arial`
    ctx.fillStyle = randomColor(30, 120)
    ctx.textBaseline = 'middle'
    ctx.textAlign = 'center'
    const charX = (width / code.length) * (i + 0.5)
    const charY = height / 2 + (Math.random() - 0.5) * 6
    ctx.save()
    ctx.translate(charX, charY)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(code.charAt(i), 0, 0)
    ctx.restore()
  }

  // 绘制干扰点
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = randomColor(100, 200)
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, Math.PI * 2)
    ctx.fill()
  }

  return {
    code,
    dataUrl: canvas.toDataURL('image/png')
  }
}

export default generateCaptcha
