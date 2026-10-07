import { useCities } from '../hooks/useCities';

/** City dropdown. Pass `allLabel` to add an "All cities" option (value ""). */
export default function CitySelect({ value, onChange, allLabel, id = 'city', required, cities: given }) {
  const { cities: loaded } = useCities();
  const cities = given || loaded;
  return (
    <select id={id} value={value ?? ''} required={required} onChange={(e) => onChange(e.target.value)}>
      {allLabel ? <option value="">{allLabel}</option> : <option value="" disabled>Select your city</option>}
      {cities.map((c) => (
        <option key={c.id} value={c.id}>
          {c.name}
          {c.state ? `, ${c.state}` : ''}
        </option>
      ))}
    </select>
  );
}
