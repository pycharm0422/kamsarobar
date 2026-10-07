import { useEffect, useState } from 'react';
import { imageUrl } from '../api';

/**
 * Photos never get stretched: one photo is shown whole at its natural shape; several photos are shown
 * as neat tiles (cropped, not squashed) and open full-size, uncropped, when tapped.
 */
export default function PostImages({ images }) {
  const [open, setOpen] = useState(null);
  if (!images?.length) return null;
  const shown = images.slice(0, 4);

  return (
    <>
      <div className={`post-images count-${shown.length}`}>
        {shown.map((img, i) => (
          <button type="button" key={img.id} className="post-image" onClick={() => setOpen(i)} aria-label="View photo">
            <img
              src={imageUrl(img)}
              alt=""
              loading="lazy"
              decoding="async"
              width={img.width || undefined}
              height={img.height || undefined}
            />
            {i === 3 && images.length > 4 && <span className="more-photos">+{images.length - 4}</span>}
          </button>
        ))}
      </div>
      {open !== null && <Lightbox images={images} index={open} onIndex={setOpen} onClose={() => setOpen(null)} />}
    </>
  );
}

function Lightbox({ images, index, onIndex, onClose }) {
  const prev = () => onIndex((index - 1 + images.length) % images.length);
  const next = () => onIndex((index + 1) % images.length);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowLeft') prev();
      if (e.key === 'ArrowRight') next();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  return (
    <div className="lightbox" onClick={onClose} role="dialog" aria-modal="true">
      <img src={imageUrl(images[index])} alt="" onClick={(e) => e.stopPropagation()} />
      <button className="lightbox-close" onClick={onClose} aria-label="Close">×</button>
      {images.length > 1 && (
        <>
          <button className="lightbox-nav left" onClick={(e) => { e.stopPropagation(); prev(); }} aria-label="Previous">‹</button>
          <button className="lightbox-nav right" onClick={(e) => { e.stopPropagation(); next(); }} aria-label="Next">›</button>
          <span className="lightbox-count">{index + 1} / {images.length}</span>
        </>
      )}
    </div>
  );
}
