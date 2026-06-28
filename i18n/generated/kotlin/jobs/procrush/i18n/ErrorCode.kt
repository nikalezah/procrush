// Generated from i18n/error-codes.yaml — do not edit manually.
package jobs.procrush.i18n

enum class ErrorCode(
    val httpStatus: Int,
    private val messageTemplate: String,
) {
    UNAUTHORIZED(401, "User is not authenticated"),
    FORBIDDEN(403, "Access denied"),
    ROLE_NOT_SELECTED(403, "User role is not selected"),
    DEV_AUTH_DISABLED(404, "Set AUTH_DEV_MODE=true in .env to enable development login"),
    REGISTRATION_CONFLICT(409, "User is already registered"),
    EMAIL_REQUIRED(400, "Email is required"),
    INVALID_EMAIL(400, "Invalid email address"),
    FIRST_NAME_REQUIRED(400, "First name is required"),
    LAST_NAME_REQUIRED(400, "Last name is required"),
    COMPANY_NAME_REQUIRED(400, "Company name is required"),
    NOT_FOUND(404, "Resource not found"),
    INVALID_REQUEST(400, "Invalid request data"),
    INVALID_ID(400, "Invalid id"),
    INVALID_SEEKER_ID(400, "Invalid seekerId"),
    INVALID_JOB_PROFILE_ID(400, "Invalid jobProfileId"),
    INVALID_OCCUPATION_ID(400, "Invalid occupationId"),
    SURVEY_ALREADY_COMPLETED(409, "Survey already completed"),
    GENERATION_IN_PROGRESS(409, "Generation already in progress"),
    USER_NOT_FOUND(404, "User not found"),
    SEEKER_NOT_FOUND(404, "Seeker not found"),
    SURVEY_NOT_FOUND(404, "Survey not found"),
    EXPERIENCE_NOT_FOUND(404, "Experience record not found"),
    EDUCATION_NOT_FOUND(404, "Education record not found"),
    OCCUPATION_NOT_FOUND(400, "Occupation not found: {{id}}"),
    PROFILE_UPDATE_FAILED(404, "Failed to update profile"),
    EMPLOYER_PROFILE_UPDATE_FAILED(404, "Failed to update company profile"),
    JOB_PROFILE_NOT_FOUND(404, "Job profile not found"),
    JOB_NOT_FOUND(404, "Job vacancy not found"),
    JOB_PROFILE_INACTIVE(400, "Job vacancy is inactive"),
    CANDIDATE_NOT_FOUND(404, "Candidate not found"),
    SEEKER_PROFILE_NOT_FOUND(404, "Seeker profile not found"),
    EMPLOYER_PROFILE_NOT_FOUND(404, "Employer profile not found"),
    PERSONALITY_TESTS_REQUIRED(400, "Complete both personality test groups to participate in matching"),
    DESIRED_OCCUPATION_REQUIRED(400, "Add this occupation to your desired positions list"),
    PROFILE_NOT_READY(400, "Profile is not ready for matching"),
    MANDATORY_TESTS_NOT_PASSED(400, "Candidate has not completed mandatory tests"),
    OCCUPATION_NOT_DESIRED(400, "Candidate has not listed this occupation"),
    NO_COMPLETED_SURVEYS(400, "No completed surveys available for interpretation"),
    LLM_TIMEOUT(500, "LLM response timed out after {{seconds}} seconds"),
    PERSONALITY_GENERATION_FAILED(500, "Personality profile generation failed"),
    PROFILE_GENERATION_FAILED(500, "Failed to generate personality profile"),
    UNKNOWN_ERROR(500, "Unknown error"),
    SURVEY_PREREQUISITES_NOT_MET(400, "Complete previous survey steps first"),
    SURVEY_NOT_STARTED(400, "Start the survey first"),
    SURVEY_SAVE_FAILED(500, "Failed to save survey answers"),
    SURVEY_COMPLETE_FAILED(500, "Failed to complete survey"),
    SURVEY_SCORING_KEYS_NOT_FOUND(500, "Survey scoring keys not found"),
    PERSONALITY_TESTS_NOT_COMPLETED(400, "Complete all personality test groups first"),
    SURVEY_UNKNOWN_TYPE(400, "Unknown survey type"),
    SURVEY_ANSWERS_MUST_BE_OBJECT(400, "Survey answers must be a JSON object"),
    SURVEY_UNSUPPORTED_TYPE(400, "Unsupported survey type: {{type}}"),
    SURVEY_NO_QUESTIONS(400, "Survey has no questions"),
    SURVEY_ANSWER_REQUIRED(400, "Answer for question {{questionId}} is required"),
    SURVEY_SELECTION_COUNT_INVALID(400, "Select {{min}} to {{max}} options"),
    SURVEY_POINTS_INVALID(400, "Points must be between 0 and {{maxPer}}"),
    SURVEY_POINTS_SUM_INVALID(400, "Points for question {{questionId}} must sum to {{total}}"),
    SURVEY_SCALE_VALUE_INVALID(400, "Answer for question {{questionId}} must be between 0 and 4"),
    SURVEY_BINARY_CHOICE_INVALID(400, "Dilemma {{questionId}}: choose option 1 or 2"),
    SURVEY_ANSWERS_NOT_FOUND(400, "Survey answers not found"),
    SURVEY_UNSUPPORTED_SCORING_LOGIC(400, "Unsupported scoring logic: {{logic}}"),
    AXIS_VALUE_OUT_OF_RANGE(400, "Axis value must be between 0 and 1"),
    LLM_JSON_PARSE_FAILED(500, "Failed to parse LLM JSON response: {{reason}}"),
    LLM_OUTPUT_FIELD_REQUIRED(500, "LLM output field {{field}} is required");

    fun formatMessage(details: Map<String, String> = emptyMap()): String {
        var result = messageTemplate
        details.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    companion object {
        fun fromName(name: String): ErrorCode? = entries.find { it.name == name }
    }
}
