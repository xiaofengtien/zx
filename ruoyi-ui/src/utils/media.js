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
