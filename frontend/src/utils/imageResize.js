/**
 * Prepares a photo for upload so it is at most 3 MB, without distorting or visibly degrading it:
 *  - Photos already <= 3 MB (JPG / PNG / WEBP / GIF) are uploaded untouched - zero quality loss.
 *  - Larger photos keep their exact aspect ratio; we first try high JPEG quality at full size, and only
 *    reduce the dimensions (in 15% steps) if that is still too big.
 *  - Downscaling is done in halving steps with high-quality smoothing, which avoids the jagged /
 *    blurry look of a single large resize.
 *  - Phone photos are rotated using their EXIF orientation, so they never appear sideways.
 */
export const MAX_IMAGE_BYTES = 3 * 1024 * 1024;

const KEEP_AS_IS = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const MAX_SIDE = 4096; // plenty for any screen, and within mobile browsers' canvas limits
const MAX_CANVAS_PIXELS = 16_000_000; // iOS Safari refuses larger canvases
const QUALITIES = [0.92, 0.86, 0.8];

export async function prepareImage(file) {
  if (!file.type.startsWith('image/') && !/\.(heic|heif)$/i.test(file.name)) {
    throw new Error(`"${file.name}" is not a photo.`);
  }
  if (file.size <= MAX_IMAGE_BYTES && KEEP_AS_IS.includes(file.type)) {
    return file;
  }

  const source = await decode(file);
  try {
    let scale = Math.min(1, MAX_SIDE / Math.max(source.width, source.height));
    for (let round = 0; round < 12; round++) {
      const width = Math.max(1, Math.round(source.width * scale));
      const height = Math.max(1, Math.round(source.height * scale));
      const canvas = resize(source.image, source.width, source.height, width, height);
      for (const quality of QUALITIES) {
        const blob = await toJpeg(canvas, quality);
        if (blob.size <= MAX_IMAGE_BYTES) {
          return new File([blob], jpegName(file.name), { type: 'image/jpeg', lastModified: Date.now() });
        }
      }
      scale *= 0.85;
    }
  } finally {
    source.close();
  }
  throw new Error(`"${file.name}" could not be made smaller than 3 MB.`);
}

async function decode(file) {
  if (typeof createImageBitmap === 'function') {
    try {
      const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
      return { image: bitmap, width: bitmap.width, height: bitmap.height, close: () => bitmap.close?.() };
    } catch {
      // fall back to <img>, which also applies EXIF orientation in modern browsers
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = reject;
      el.src = url;
    });
    return { image: img, width: img.naturalWidth, height: img.naturalHeight, close: () => URL.revokeObjectURL(url) };
  } catch {
    URL.revokeObjectURL(url);
    throw new Error(`Can't read "${file.name}". Please choose a JPG or PNG photo.`);
  }
}

function resize(image, sourceWidth, sourceHeight, targetWidth, targetHeight) {
  let current = image;
  let w = sourceWidth;
  let h = sourceHeight;
  // Halve while still more than 2x the target size.
  while (w / 2 >= targetWidth && h / 2 >= targetHeight) {
    let nw = Math.round(w / 2);
    let nh = Math.round(h / 2);
    if (nw * nh > MAX_CANVAS_PIXELS) {
      const fit = Math.sqrt(MAX_CANVAS_PIXELS / (nw * nh));
      nw = Math.floor(nw * fit);
      nh = Math.floor(nh * fit);
    }
    const step = canvas(nw, nh);
    draw(step, current, nw, nh, false);
    current = step;
    w = nw;
    h = nh;
  }
  const out = canvas(targetWidth, targetHeight);
  draw(out, current, targetWidth, targetHeight, true);
  return out;
}

function canvas(width, height) {
  const c = document.createElement('canvas');
  c.width = width;
  c.height = height;
  return c;
}

function draw(target, image, width, height, whiteBackground) {
  const ctx = target.getContext('2d');
  if (whiteBackground) {
    // JPEG has no transparency: put transparent PNG areas on white instead of black.
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, width, height);
  }
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(image, 0, 0, width, height);
}

function toJpeg(c, quality) {
  return new Promise((resolve, reject) =>
    c.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Could not process the photo.'))), 'image/jpeg', quality),
  );
}

function jpegName(name) {
  return name.replace(/\.[^.]+$/, '') + '.jpg';
}
