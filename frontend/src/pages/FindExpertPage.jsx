import DirectorySearch from '../components/DirectorySearch';
import { directoryApi, profileApi } from '../api';
import { adviceMessage } from '../utils/whatsapp';

const config = {
  title: 'Find an expert',
  subtitle: 'Need guidance? Search by expertise - career, a technology, exams, business, health and more.',
  label: 'Expertise',
  placeholder: 'e.g. Java, UPSC, Startups, Medicine',
  param: 'expertise',
  search: directoryApi.experts,
  suggest: profileApi.suggestExpertise,
  actionLabel: 'Ask for advice',
  emptyIcon: '💡',
  emptyText: 'No member has listed this expertise yet. Try a related word.',
  buildMessage: ({ member, me, myProfile, term }) => adviceMessage({ member, me, myProfile, topic: term }),
};

export default function FindExpertPage() {
  return <DirectorySearch config={config} />;
}
