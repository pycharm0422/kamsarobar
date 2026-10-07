import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';

/**
 * Same rules as the website: photos up to 3 MB are uploaded untouched (no quality loss); bigger ones keep their
 * exact shape and are re-saved as high-quality JPEG, shrinking the size in 15% steps only if still too big.
 */
export const MAX_IMAGE_BYTES = 3 * 1024 * 1024;
const KEEP_AS_IS = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const MAX_SIDE = 4096;
const QUALITIES = [0.92, 0.86, 0.8];

async function sizeOf(uri) {
  const blob = await (await fetch(uri)).blob();
  return blob.size;
}

/** @param asset an asset from expo-image-picker. Returns { uri, name, type } ready for upload. */
export async function prepareImage(asset) {
  const type = asset.mimeType || 'image/jpeg';
  const name = asset.fileName || `photo-${Date.now()}.jpg`;
  const size = asset.fileSize ?? (await sizeOf(asset.uri));
  if (size <= MAX_IMAGE_BYTES && KEEP_AS_IS.includes(type)) {
    return { uri: asset.uri, name, type };
  }

  const longest = Math.max(asset.width || MAX_SIDE, asset.height || MAX_SIDE);
  let scale = Math.min(1, MAX_SIDE / longest);
  for (let round = 0; round < 12; round++) {
    const context = ImageManipulator.manipulate(asset.uri);
    if (scale < 1 && asset.width && asset.height) {
      // Resize by the longer side only, so the other side follows and the shape never changes.
      context.resize(asset.width >= asset.height ? { width: Math.round(asset.width * scale) } : { height: Math.round(asset.height * scale) });
    }
    const image = await context.renderAsync();
    for (const compress of QUALITIES) {
      const result = await image.saveAsync({ compress, format: SaveFormat.JPEG });
      if ((await sizeOf(result.uri)) <= MAX_IMAGE_BYTES) {
        return { uri: result.uri, name: name.replace(/\.[^.]+$/, '') + '.jpg', type: 'image/jpeg' };
      }
    }
    scale *= 0.85;
  }
  throw new Error('This photo could not be made smaller than 3 MB.');
}
