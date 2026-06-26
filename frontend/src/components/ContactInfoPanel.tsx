import type {EmployerContactDto, SeekerContactDto} from '../api/types'

interface ContactInfoPanelProps {
  contactInfo: EmployerContactDto | SeekerContactDto
  perspective: 'seeker' | 'employer'
}

function isEmployerContact(
  contact: EmployerContactDto | SeekerContactDto,
): contact is EmployerContactDto {
  return 'companyName' in contact
}

export function ContactInfoPanel({contactInfo, perspective}: ContactInfoPanelProps) {
  const title = perspective === 'seeker' ? '💕 Контакты работодателя' : '💕 Контакты кандидата'

  return (
    <div className="w-full rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50 to-brand-50 p-4 text-sm">
      <p className="font-semibold text-emerald-900">{title}</p>
      <ul className="mt-2 flex flex-col gap-1.5 text-emerald-800">
        {isEmployerContact(contactInfo) ? (
          <>
            <li className="font-medium">{contactInfo.companyName}</li>
            {contactInfo.phone != null && contactInfo.phone !== '' && (
              <li>
                <a href={`tel:${contactInfo.phone}`} className="underline hover:text-emerald-900">
                  {contactInfo.phone}
                </a>
              </li>
            )}
            {contactInfo.emailContact != null && contactInfo.emailContact !== '' && (
              <li>
                <a
                  href={`mailto:${contactInfo.emailContact}`}
                  className="underline hover:text-emerald-900"
                >
                  {contactInfo.emailContact}
                </a>
              </li>
            )}
            {contactInfo.website != null && contactInfo.website !== '' && (
              <li>
                <a
                  href={contactInfo.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline hover:text-emerald-900"
                >
                  {contactInfo.website}
                </a>
              </li>
            )}
          </>
        ) : (
          <>
            <li className="font-medium">
              {contactInfo.firstName} {contactInfo.lastName}
            </li>
            {contactInfo.phone != null && contactInfo.phone !== '' && (
              <li>
                <a href={`tel:${contactInfo.phone}`} className="underline hover:text-emerald-900">
                  {contactInfo.phone}
                </a>
              </li>
            )}
            {contactInfo.telegram != null && contactInfo.telegram !== '' && (
              <li>{contactInfo.telegram}</li>
            )}
            {contactInfo.linkedin != null && contactInfo.linkedin !== '' && (
              <li>
                <a
                  href={contactInfo.linkedin}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline hover:text-emerald-900"
                >
                  LinkedIn
                </a>
              </li>
            )}
          </>
        )}
      </ul>
    </div>
  )
}
