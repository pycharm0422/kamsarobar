import { useEffect, useState } from 'react';
import { cityApi } from '../api';

let cache = null;

/** Call after switching to another server. */
export function resetCities() {
  cache = null;
}

export function useCities(reloadKey) {
  const [cities, setCities] = useState(cache || []);
  useEffect(() => {
    if (cache) {
      setCities(cache);
      return;
    }
    cityApi.list().then((list) => {
      cache = list;
      setCities(list);
    }).catch(() => {});
  }, [reloadKey]);
  return cities;
}
