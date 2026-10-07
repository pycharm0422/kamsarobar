import DirectorySearch from '../components/DirectorySearch';
import { directoryApi, profileApi } from '../api';
import { referralMessage } from '../utils/whatsapp';

const config = {
  title: 'Find a referral',
  subtitle: 'Saw an opening? Type the company name to see community members who can refer you there.',
  label: 'Company name',
  placeholder: 'e.g. Google, TCS, Amazon',
  param: 'company',
  search: directoryApi.referrers,
  suggest: profileApi.suggestCompanies,
  extraFields: [
    { name: 'role', label: 'Job role (optional)', placeholder: 'e.g. Data Analyst' },
    { name: 'jobLink', label: 'Job link (optional)', placeholder: 'https://...' },
  ],
  actionLabel: 'Ask for referral',
  emptyIcon: '🏢',
  emptyText: 'No member has listed this company yet. Try a shorter name, or another city.',
  buildMessage: ({ member, me, myProfile, term, role, jobLink }) =>
    referralMessage({ member, me, myProfile, company: term, role, jobLink }),
};

export default function FindReferralPage() {
  return <DirectorySearch config={config} />;
}
