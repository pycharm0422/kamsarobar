import { useEffect, useState } from 'react';
import { useDebounce } from '../hooks/useDebounce';

/** Single text input with a dropdown of suggestions from the server. */
export default function SuggestInput({ id, value, onChange, suggest, placeholder }) {
  const [suggestions, setSuggestions] = useState([]);
  const [focused, setFocused] = useState(false);
  const query = useDebounce(value, 250);

  useEffect(() => {
    if (!suggest || query.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    let active = true;
    suggest(query.trim())
      .then((list) => active && setSuggestions(list.filter((s) => s.toLowerCase() !== query.trim().toLowerCase())))
      .catch(() => active && setSuggestions([]));
    return () => {
      active = false;
    };
  }, [query, suggest]);

  return (
    <div className="tag-input">
      <input
        id={id}
        value={value}
        placeholder={placeholder}
        autoComplete="off"
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
      />
      {focused && suggestions.length > 0 && (
        <ul className="suggestions">
          {suggestions.map((s) => (
            <li key={s}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(s);
                  setSuggestions([]);
                }}
              >
                {s}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
