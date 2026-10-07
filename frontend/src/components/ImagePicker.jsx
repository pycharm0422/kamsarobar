import { useEffect, useRef, useState } from 'react';
import { imageApi, imageUrl } from '../api';
import { errorMessage } from '../utils/errors';
import { prepareImage } from '../utils/imageResize';

const newKey = () => (globalThis.crypto?.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`);

/**
 * Pick photos, shrink each to <= 3 MB in the browser, and upload it. `images` are the uploaded photos
 * ({ id, url, width, height }); `onBusyChange(true)` while anything is still processing.
 */
export default function ImagePicker({ images, onChange, onBusyChange, max = 6 }) {
  const [pending, setPending] = useState([]);
  const [error, setError] = useState('');
  const latest = useRef(images);
  latest.current = images;

  useEffect(() => {
    onBusyChange?.(pending.length > 0);
  }, [pending.length, onBusyChange]);

  const addFiles = async (fileList) => {
    setError('');
    const room = max - latest.current.length - pending.length;
    const files = Array.from(fileList).slice(0, Math.max(0, room));
    if (fileList.length > files.length) setError(`You can add up to ${max} photos.`);

    // One at a time keeps memory low on phones.
    for (const file of files) {
      const key = newKey();
      const preview = URL.createObjectURL(file);
      setPending((p) => [...p, { key, preview, stage: 'Optimizing…' }]);
      try {
        const ready = await prepareImage(file);
        setPending((p) => p.map((x) => (x.key === key ? { ...x, stage: 'Uploading…' } : x)));
        const uploaded = await imageApi.upload(ready);
        latest.current = [...latest.current, uploaded];
        onChange(latest.current);
      } catch (e) {
        setError(e.response || e.code ? errorMessage(e) : e.message);
      } finally {
        URL.revokeObjectURL(preview);
        setPending((p) => p.filter((x) => x.key !== key));
      }
    }
  };

  const remove = (id) => onChange(images.filter((img) => img.id !== id));
  const full = images.length + pending.length >= max;

  return (
    <div className="image-picker">
      <div className="thumbs">
        {images.map((img) => (
          <div key={img.id} className="thumb">
            <img src={imageUrl(img)} alt="" />
            <button type="button" className="thumb-remove" onClick={() => remove(img.id)} aria-label="Remove photo">×</button>
          </div>
        ))}
        {pending.map((p) => (
          <div key={p.key} className="thumb pending">
            <img src={p.preview} alt="" />
            <span className="thumb-stage">{p.stage}</span>
          </div>
        ))}
        {!full && (
          <label className="thumb add-photo">
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={(e) => {
                addFiles(e.target.files);
                e.target.value = '';
              }}
            />
            <span>📷</span>
            <small>Add photos</small>
          </label>
        )}
      </div>
      {error && <p className="field-error">{error}</p>}
    </div>
  );
}
