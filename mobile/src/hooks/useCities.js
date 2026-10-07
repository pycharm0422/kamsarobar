import { useEffect, useState } from 'react';
import { cityApi } from '../api';

let cache = null;

export function useCities() {
  const [cities, setCities] = useState(cache || []);
  useEffect(() => {
    if (cache) return;
    cityApi.list().then((list) => {
      cache = list;
      setCities(list);
    }).catch(() => {});
  }, []);
  return cities;
}
