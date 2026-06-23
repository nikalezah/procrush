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

export function ContactInfoPanel({ contactInfo, perspective }: ContactInfoPanelProps) {
  const title =
    perspective === 'seeker' ? 'Контакты работодателя' : 'Контакты кандидата'

  return (
    <div className="w-full rounded-lg border border-green-200 bg-green-50 p-3 text-sm">
      <p className="font-medium text-green-900">{title}</p>
      <ul className="mt-2 flex flex-col gap-1 text-green-800">
        {isEmployerContact(contactInfo) ? (
          <>
            <li>{contactInfo.companyName}</li>
            {contactInfo.phone != null && contactInfo.phone !== '' && (
              <li>
                <a href={`tel:${contactInfo.phone}`} className="underline">
                  {contactInfo.phone}
                </a>
              </li>
            )}
            {contactInfo.emailContact != null && contactInfo.emailContact !== '' && (
              <li>
                <a href={`mailto:${contactInfo.emailContact}`} className="underline">
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
                  className="underline"
                >
                  {contactInfo.website}
                </a>
              </li>
            )}
          </>
        ) : (
          <>
            <li>
              {contactInfo.firstName} {contactInfo.lastName}
            </li>
            {contactInfo.phone != null && contactInfo.phone !== '' && (
              <li>
                <a href={`tel:${contactInfo.phone}`} className="underline">
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
                  className="underline"
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
