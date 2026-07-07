export function encodeUrlFileName(url) {
  if (!url) return url

  try {
    const urlObj = new URL(url)
    const pathParts = urlObj.pathname.split('/').filter(part => part)
    if (pathParts.length === 0) return url

    const fileName = pathParts[pathParts.length - 1]
    let encodedFileName = fileName

    if (!fileName.includes('%')) {
      try {
        const decoded = decodeURIComponent(fileName)
        encodedFileName = encodeURIComponent(decoded)
      } catch (e) {
        encodedFileName = encodeURIComponent(fileName)
      }
    }

    pathParts[pathParts.length - 1] = encodedFileName
    urlObj.pathname = '/' + pathParts.join('/')

    return urlObj.toString()
  } catch (e) {
    const match = url.match(/^(https?:\/\/[^\/]+)(\/.*)$/)
    if (match) {
      const [, baseUrl, path] = match
      const pathParts = path.split('/').filter(part => part)
      if (pathParts.length > 0) {
        const fileName = pathParts[pathParts.length - 1]
        if (!fileName.includes('%')) {
          pathParts[pathParts.length - 1] = encodeURIComponent(fileName)
        }
        return baseUrl + '/' + pathParts.join('/')
      }
    }

    return url
  }
}

export function normalizeMediaUrl(url) {
  if (!url) return ''

  let absoluteUrl = url
  if (!absoluteUrl.startsWith('http://') && !absoluteUrl.startsWith('https://')) {
    if (absoluteUrl.startsWith('/')) {
      absoluteUrl = process.env.VUE_APP_BASE_API + absoluteUrl
    } else {
      absoluteUrl = `${process.env.VUE_APP_BASE_API}/${absoluteUrl}`
    }
  }

  return encodeUrlFileName(absoluteUrl)
}

/**
 * 获取音频文件时长（秒）
 * 使用 HTML5 Audio API 加载音频元数据
 * @param {string} url 音频 URL
 * @param {number} timeout 超时时间（毫秒），默认 10 秒
 * @returns {Promise<number|null>} 音频时长（秒），获取失败返回 null
 */
export function getAudioDuration(url, timeout = 10000) {
  return new Promise((resolve) => {
    if (!url) {
      resolve(null)
      return
    }

    const audio = new Audio()
    let resolved = false

    // 超时处理
    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true
        console.warn('[getAudioDuration] 获取音频时长超时:', url)
        audio.src = '' // 停止加载
        resolve(null)
      }
    }, timeout)

    // 成功获取元数据
    audio.addEventListener('loadedmetadata', () => {
      if (!resolved) {
        resolved = true
        clearTimeout(timer)
        const duration = Math.round(audio.duration)
        console.log('[getAudioDuration] 获取音频时长成功:', url, duration, '秒')
        resolve(duration)
      }
    })

    // 加载错误
    audio.addEventListener('error', (e) => {
      if (!resolved) {
        resolved = true
        clearTimeout(timer)
        console.warn('[getAudioDuration] 获取音频时长失败:', url, e)
        resolve(null)
      }
    })

    // 开始加载（只加载元数据，不下载完整音频）
    audio.preload = 'metadata'
    audio.src = normalizeMediaUrl(url)
  })
}

/**
 * 批量获取多个音频的时长
 * @param {Array<{key: string, url: string}>} items 音频列表
 * @returns {Promise<Object>} key -> duration 的映射
 */
export async function getAudioDurations(items) {
  const results = {}
  await Promise.all(
    items.map(async (item) => {
      if (item.url) {
        results[item.key] = await getAudioDuration(item.url)
      }
    })
  )
  return results
}
