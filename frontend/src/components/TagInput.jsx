import { useEffect, useState } from 'react';
import { useDebounce } from '../hooks/useDebounce';

/**
 * Chips input: type and press Enter (or comma) to add. Optional async `suggest(query)` shows
 * names other members already used, so everyone spells "Tata Consultancy Services" the same way.
 */
export default function TagInput({ id, value, onChange, placeholder, suggest, max = 20 }) {
  const [text, setText] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const query = useDebounce(text, 250);

  useEffect(() => {
    if (!suggest || query.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    let active = true;
    suggest(query.trim())
      .then((list) => active && setSuggestions(list.filter((s) => !contains(value, s))))
      .catch(() => active && setSuggestions([]));
    return () => {
      active = false;
    };
  }, [query, suggest, value]);

  const add = (raw) => {
    const tag = raw.trim().replace(/\s+/g, ' ');
    if (tag && !contains(value, tag) && value.length < max) {
      onChange([...value, tag]);
    }
    setText('');
    setSuggestions([]);
  };

  const onKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      add(text);
    } else if (e.key === 'Backspace' && !text && value.length) {
      onChange(value.slice(0, -1));
    }
  };

  return (
    <div className="tag-input">
      <div className="tag-box">
        {value.map((tag) => (
          <span key={tag} className="chip">
            {tag}
            <button type="button" onClick={() => onChange(value.filter((t) => t !== tag))} aria-label={`Remove ${tag}`}>
              ×
            </button>
          </span>
        ))}
        <input
          id={id}
          value={text}
          placeholder={value.length ? '' : placeholder}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={onKeyDown}
          onBlur={() => text && add(text)}
          autoComplete="off"
        />
      </div>
      {suggestions.length > 0 && (
        <ul className="suggestions">
          {suggestions.map((s) => (
            <li key={s}>
              <button type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => add(s)}>
                {s}
              </button>
            </li>
          ))}
        </ul>
      )}
      <small className="muted">Press Enter after each one.</small>
    </div>
  );
}

const contains = (list, tag) => list.some((t) => t.toLowerCase() === tag.toLowerCase());
