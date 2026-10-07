import { useEffect, useState } from 'react';
import { cityApi } from '../api';

let cache = null;

/** Active cities, fetched once per page load and shared by every component. */
export function useCities() {
  const [cities, setCities] = useState(cache || []);
  const [loading, setLoading] = useState(!cache);

  useEffect(() => {
    if (cache) return;
    cityApi
      .list()
      .then((list) => {
        cache = list;
        setCities(list);
      })
      .finally(() => setLoading(false));
  }, []);

  return { cities, loading };
}

export function invalidateCities() {
  cache = null;
}
