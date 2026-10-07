import { useCities } from '../hooks/useCities';
import OptionPicker from './OptionPicker';

export default function CityPicker({ label, value, onChange, allLabel, compact, reloadKey }) {
  const cities = useCities(reloadKey);
  const options = [
    ...(allLabel ? [{ value: '', label: allLabel }] : []),
    ...cities.map((c) => ({ value: String(c.id), label: c.state ? `${c.name}, ${c.state}` : c.name })),
  ];
  return <OptionPicker label={label} value={value} options={options} onChange={onChange} placeholder="Select your city" compact={compact} />;
}
