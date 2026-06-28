package jobs.procrush.api.mapper

import jobs.procrush.auth.AuthUserDto
import jobs.procrush.auth.CompleteRegistrationRequest
import jobs.procrush.auth.DevLoginRequest
import jobs.procrush.auth.MeResponse
import jobs.procrush.auth.UserRole
import jobs.procrush.employer.dto.CreateJobProfileRequest
import jobs.procrush.employer.dto.EmployerDashboardDto
import jobs.procrush.employer.dto.EmployerProfileDto
import jobs.procrush.employer.dto.JobProfileDto
import jobs.procrush.employer.dto.UpdateEmployerProfileRequest
import jobs.procrush.employer.dto.UpdateJobProfileRequest
import jobs.procrush.matching.dto.CandidateRecommendationDto
import jobs.procrush.matching.dto.EmployerCandidatesOverviewDto
import jobs.procrush.matching.dto.EmployerContactDto
import jobs.procrush.matching.dto.EmployerInterestsResponseDto
import jobs.procrush.matching.dto.InterestStatus
import jobs.procrush.matching.dto.JobRecommendationDto
import jobs.procrush.matching.dto.MatchInterestCountDto
import jobs.procrush.matching.dto.SeekerContactDto
import jobs.procrush.matching.dto.SeekerInterestsResponseDto
import jobs.procrush.personality.dto.PersonalityAxesDto
import jobs.procrush.personality.dto.PersonalityCategoryDto
import jobs.procrush.personality.dto.PersonalityItemDto
import jobs.procrush.personality.dto.PersonalityPreviewDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.dto.PersonalitySectionDto
import jobs.procrush.personality.dto.PersonalityTraitDetailsDto
import jobs.procrush.personality.dto.PersonalityTraitDto
import jobs.procrush.personality.dto.SucceedThroughDto
import jobs.procrush.seeker.dto.CreateSeekerEducationRequest
import jobs.procrush.seeker.dto.CreateSeekerExperienceRequest
import jobs.procrush.seeker.dto.SeekerDashboardDto
import jobs.procrush.seeker.dto.SeekerDesiredPositionsResponse
import jobs.procrush.seeker.dto.SeekerEducationDto
import jobs.procrush.seeker.dto.SeekerExperienceDto
import jobs.procrush.seeker.dto.SeekerPositionsOverviewDto
import jobs.procrush.seeker.dto.SeekerProfileDto
import jobs.procrush.seeker.dto.SeekerSkillsResponse
import jobs.procrush.seeker.dto.UpdateSeekerDesiredPositionsRequest
import jobs.procrush.seeker.dto.UpdateSeekerEducationRequest
import jobs.procrush.seeker.dto.UpdateSeekerExperienceRequest
import jobs.procrush.seeker.dto.UpdateSeekerProfileRequest
import jobs.procrush.seeker.dto.UpdateSeekerSkillsRequest
import jobs.procrush.shared.dto.OccupationDto
import jobs.procrush.shared.dto.SkillDto
import jobs.procrush.shared.dto.SuperpowerAndTalentDto
import jobs.procrush.survey.dto.CompleteSurveyResponseDto
import jobs.procrush.survey.dto.GlossaryTermDto
import jobs.procrush.survey.dto.SaveSurveyAnswersRequest
import jobs.procrush.survey.dto.SurveyDetailDto
import jobs.procrush.survey.dto.SurveyGroupDto
import jobs.procrush.survey.dto.SurveyGroupsResponseDto
import jobs.procrush.survey.dto.SurveyListItemDto
import jobs.procrush.survey.dto.SurveyLlmContextDto
import jobs.procrush.survey.dto.SurveyLlmContextItemDto
import jobs.procrush.survey.dto.SurveyStatus
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import jobs.procrush.api.generated.auth_models_yaml.auth_models.AuthUserDto as ApiAuthUserDto
import jobs.procrush.api.generated.auth_models_yaml.auth_models.CompleteRegistrationRequest as ApiCompleteRegistrationRequest
import jobs.procrush.api.generated.auth_models_yaml.auth_models.DevLoginRequest as ApiDevLoginRequest
import jobs.procrush.api.generated.auth_models_yaml.auth_models.MeResponse as ApiMeResponse
import jobs.procrush.api.generated.common_models_yaml.common_models.InterestStatus as ApiInterestStatus
import jobs.procrush.api.generated.common_models_yaml.common_models.PersonalityProfileStatus as ApiPersonalityProfileStatus
import jobs.procrush.api.generated.common_models_yaml.common_models.SurveyStatus as ApiSurveyStatus
import jobs.procrush.api.generated.common_models_yaml.common_models.UserRole as ApiUserRole
import jobs.procrush.api.generated.employer_models_yaml.employer_models.CreateJobProfileRequest as ApiCreateJobProfileRequest
import jobs.procrush.api.generated.employer_models_yaml.employer_models.EmployerDashboardDto as ApiEmployerDashboardDto
import jobs.procrush.api.generated.employer_models_yaml.employer_models.EmployerProfileDto as ApiEmployerProfileDto
import jobs.procrush.api.generated.employer_models_yaml.employer_models.JobProfileDto as ApiJobProfileDto
import jobs.procrush.api.generated.employer_models_yaml.employer_models.UpdateEmployerProfileRequest as ApiUpdateEmployerProfileRequest
import jobs.procrush.api.generated.employer_models_yaml.employer_models.UpdateJobProfileRequest as ApiUpdateJobProfileRequest
import jobs.procrush.api.generated.matching_models_yaml.matching_models.CandidateRecommendationDto as ApiCandidateRecommendationDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.EmployerCandidatesOverviewDto as ApiEmployerCandidatesOverviewDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.EmployerContactDto as ApiEmployerContactDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.EmployerInterestsResponseDto as ApiEmployerInterestsResponseDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.JobRecommendationDto as ApiJobRecommendationDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.MatchInterestCountDto as ApiMatchInterestCountDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.SeekerContactDto as ApiSeekerContactDto
import jobs.procrush.api.generated.matching_models_yaml.matching_models.SeekerInterestsResponseDto as ApiSeekerInterestsResponseDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityAxesDto as ApiPersonalityAxesDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityCategoryDto as ApiPersonalityCategoryDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityGenerationStatusResponse as ApiPersonalityGenerationStatusResponse
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityItemDto as ApiPersonalityItemDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityPreviewDto as ApiPersonalityPreviewDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalitySectionDto as ApiPersonalitySectionDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityTraitDetailsDto as ApiPersonalityTraitDetailsDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.PersonalityTraitDto as ApiPersonalityTraitDto
import jobs.procrush.api.generated.personality_models_yaml.personality_models.SucceedThroughDto as ApiSucceedThroughDto
import jobs.procrush.api.generated.reference_models_yaml.reference_models.OccupationDto as ApiOccupationDto
import jobs.procrush.api.generated.reference_models_yaml.reference_models.SkillDto as ApiSkillDto
import jobs.procrush.api.generated.reference_models_yaml.reference_models.SuperpowerAndTalentDto as ApiSuperpowerAndTalentDto
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.CreateSeekerEducationRequest as ApiCreateSeekerEducationRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.CreateSeekerExperienceRequest as ApiCreateSeekerExperienceRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerDashboardDto as ApiSeekerDashboardDto
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerDesiredPositionsResponse as ApiSeekerDesiredPositionsResponse
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerEducationDto as ApiSeekerEducationDto
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerExperienceDto as ApiSeekerExperienceDto
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerPositionsOverviewDto as ApiSeekerPositionsOverviewDto
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerProfileDto as ApiSeekerProfileDto
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.SeekerSkillsResponse as ApiSeekerSkillsResponse
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerDesiredPositionsRequest as ApiUpdateSeekerDesiredPositionsRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerEducationRequest as ApiUpdateSeekerEducationRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerExperienceRequest as ApiUpdateSeekerExperienceRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerProfileRequest as ApiUpdateSeekerProfileRequest
import jobs.procrush.api.generated.seeker_models_yaml.seeker_models.UpdateSeekerSkillsRequest as ApiUpdateSeekerSkillsRequest
import jobs.procrush.api.generated.survey_models_yaml.survey_models.CompleteSurveyResponseDto as ApiCompleteSurveyResponseDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.GlossaryTermDto as ApiGlossaryTermDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SaveSurveyAnswersRequest as ApiSaveSurveyAnswersRequest
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SurveyDetailDto as ApiSurveyDetailDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SurveyGroupDto as ApiSurveyGroupDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SurveyGroupsResponseDto as ApiSurveyGroupsResponseDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SurveyListItemDto as ApiSurveyListItemDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SurveyLlmContextDto as ApiSurveyLlmContextDto
import jobs.procrush.api.generated.survey_models_yaml.survey_models.SurveyLlmContextItemDto as ApiSurveyLlmContextItemDto

fun UserRole.toApi(): ApiUserRole = ApiUserRole.valueOf(name)

fun ApiUserRole.toContract(): UserRole = UserRole.valueOf(name)

fun InterestStatus.toApi(): ApiInterestStatus = ApiInterestStatus.valueOf(name)

fun ApiInterestStatus.toContract(): InterestStatus = InterestStatus.valueOf(name)

fun SurveyStatus.toApi(): ApiSurveyStatus = ApiSurveyStatus.valueOf(name)

fun ApiSurveyStatus.toContract(): SurveyStatus = SurveyStatus.valueOf(name)

fun PersonalityProfileStatus.toApi(): ApiPersonalityProfileStatus = ApiPersonalityProfileStatus.valueOf(name)

fun ApiPersonalityProfileStatus.toContract(): PersonalityProfileStatus = PersonalityProfileStatus.valueOf(name)

fun AuthUserDto.toApi(): ApiAuthUserDto =
    ApiAuthUserDto(
        id = id,
        email = email,
        profileName = profileName,
        role = role?.toApi(),
    )

fun ApiAuthUserDto.toContract(): AuthUserDto =
    AuthUserDto(
        id = id,
        email = email,
        profileName = profileName,
        role = role?.toContract(),
    )

fun DevLoginRequest.toApi(): ApiDevLoginRequest = ApiDevLoginRequest(email = email)

fun ApiDevLoginRequest.toContract(): DevLoginRequest = DevLoginRequest(email = email)

fun CompleteRegistrationRequest.toApi(): ApiCompleteRegistrationRequest =
    ApiCompleteRegistrationRequest(
        email = email,
        role = role.toApi(),
        firstName = firstName,
        lastName = lastName,
        middleName = middleName,
        companyName = companyName,
    )

fun ApiCompleteRegistrationRequest.toContract(): CompleteRegistrationRequest =
    CompleteRegistrationRequest(
        email = email,
        role = role.toContract(),
        firstName = firstName,
        lastName = lastName,
        middleName = middleName,
        companyName = companyName,
    )

fun MeResponse.toApi(): ApiMeResponse =
    ApiMeResponse(
        user = user?.toApi(),
    )

fun ApiMeResponse.toContract(): MeResponse = MeResponse(user = user?.toContract())

fun OccupationDto.toApi(): ApiOccupationDto =
    ApiOccupationDto(
        id = id,
        parentId = parentId,
        name = name,
        isLeaf = isLeaf,
    )

fun ApiOccupationDto.toContract(): OccupationDto =
    OccupationDto(
        id = id,
        parentId = parentId,
        name = name,
        isLeaf = isLeaf,
    )

fun SkillDto.toApi(): ApiSkillDto = ApiSkillDto(id = id, name = name)

fun ApiSkillDto.toContract(): SkillDto = SkillDto(id = id, name = name)

fun SuperpowerAndTalentDto.toApi(): ApiSuperpowerAndTalentDto =
    ApiSuperpowerAndTalentDto(
        id = id,
        name = name,
        isPronounced = isPronounced,
    )

fun ApiSuperpowerAndTalentDto.toContract(): SuperpowerAndTalentDto =
    SuperpowerAndTalentDto(
        id = id,
        name = name,
        isPronounced = isPronounced,
    )

fun EmployerContactDto.toApi(): ApiEmployerContactDto =
    ApiEmployerContactDto(
        companyName = companyName,
        phone = phone,
        emailContact = emailContact,
        website = website,
    )

fun ApiEmployerContactDto.toContract(): EmployerContactDto =
    EmployerContactDto(
        companyName = companyName,
        phone = phone,
        emailContact = emailContact,
        website = website,
    )

fun SeekerContactDto.toApi(): ApiSeekerContactDto =
    ApiSeekerContactDto(
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        telegram = telegram,
        linkedin = linkedin,
    )

fun ApiSeekerContactDto.toContract(): SeekerContactDto =
    SeekerContactDto(
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        telegram = telegram,
        linkedin = linkedin,
    )

fun JobRecommendationDto.toApi(): ApiJobRecommendationDto =
    ApiJobRecommendationDto(
        id = id,
        companyName = companyName,
        positionName = positionName,
        description = description,
        matchScore = matchScore,
        matchScoreDisplay = matchScoreDisplay,
        interestStatus = interestStatus.toApi(),
        contactInfo = contactInfo?.toApi(),
    )

fun ApiJobRecommendationDto.toContract(): JobRecommendationDto =
    JobRecommendationDto(
        id = id,
        companyName = companyName,
        positionName = positionName,
        description = description,
        matchScore = matchScore,
        matchScoreDisplay = matchScoreDisplay,
        interestStatus = interestStatus.toContract(),
        contactInfo = contactInfo?.toContract(),
    )

fun CandidateRecommendationDto.toApi(): ApiCandidateRecommendationDto =
    ApiCandidateRecommendationDto(
        id = id,
        firstName = firstName,
        lastName = lastName,
        positionName = positionName,
        skills = skills,
        matchScore = matchScore,
        matchScoreDisplay = matchScoreDisplay,
        interestStatus = interestStatus.toApi(),
        contactInfo = contactInfo?.toApi(),
    )

fun ApiCandidateRecommendationDto.toContract(): CandidateRecommendationDto =
    CandidateRecommendationDto(
        id = id,
        firstName = firstName,
        lastName = lastName,
        positionName = positionName,
        skills = skills,
        matchScore = matchScore,
        matchScoreDisplay = matchScoreDisplay,
        interestStatus = interestStatus.toContract(),
        contactInfo = contactInfo?.toContract(),
    )

fun SeekerInterestsResponseDto.toApi(): ApiSeekerInterestsResponseDto =
    ApiSeekerInterestsResponseDto(
        respondedOutside = respondedOutside.map { it.toApi() },
        mutualOutside = mutualOutside.map { it.toApi() },
    )

fun ApiSeekerInterestsResponseDto.toContract(): SeekerInterestsResponseDto =
    SeekerInterestsResponseDto(
        respondedOutside = respondedOutside.map { it.toContract() },
        mutualOutside = mutualOutside.map { it.toContract() },
    )

fun EmployerInterestsResponseDto.toApi(): ApiEmployerInterestsResponseDto =
    ApiEmployerInterestsResponseDto(
        respondedOutside = respondedOutside.map { it.toApi() },
        mutualOutside = mutualOutside.map { it.toApi() },
    )

fun ApiEmployerInterestsResponseDto.toContract(): EmployerInterestsResponseDto =
    EmployerInterestsResponseDto(
        respondedOutside = respondedOutside.map { it.toContract() },
        mutualOutside = mutualOutside.map { it.toContract() },
    )

fun EmployerCandidatesOverviewDto.toApi(): ApiEmployerCandidatesOverviewDto =
    ApiEmployerCandidatesOverviewDto(
        candidates = candidates.map { it.toApi() },
        interests = interests.toApi(),
    )

fun ApiEmployerCandidatesOverviewDto.toContract(): EmployerCandidatesOverviewDto =
    EmployerCandidatesOverviewDto(
        candidates = candidates.map { it.toContract() },
        interests = interests.toContract(),
    )

fun MatchInterestCountDto.toApi(): ApiMatchInterestCountDto = ApiMatchInterestCountDto(count = count)

fun ApiMatchInterestCountDto.toContract(): MatchInterestCountDto = MatchInterestCountDto(count = count)

fun SeekerProfileDto.toApi(): ApiSeekerProfileDto =
    ApiSeekerProfileDto(
        id = id,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        phone = phone,
        telegram = telegram,
        linkedin = linkedin,
    )

fun ApiSeekerProfileDto.toContract(): SeekerProfileDto =
    SeekerProfileDto(
        id = id,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        phone = phone,
        telegram = telegram,
        linkedin = linkedin,
    )

fun UpdateSeekerProfileRequest.toApi(): ApiUpdateSeekerProfileRequest =
    ApiUpdateSeekerProfileRequest(
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        phone = phone,
        telegram = telegram,
        linkedin = linkedin,
    )

fun ApiUpdateSeekerProfileRequest.toContract(): UpdateSeekerProfileRequest =
    UpdateSeekerProfileRequest(
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        phone = phone,
        telegram = telegram,
        linkedin = linkedin,
    )

fun SeekerExperienceDto.toApi(): ApiSeekerExperienceDto =
    ApiSeekerExperienceDto(
        id = id,
        companyName = companyName,
        position = position,
        description = description,
        startDate = startDate,
        endDate = endDate,
    )

fun ApiSeekerExperienceDto.toContract(): SeekerExperienceDto =
    SeekerExperienceDto(
        id = id,
        companyName = companyName,
        position = position,
        description = description,
        startDate = startDate,
        endDate = endDate,
    )

fun CreateSeekerExperienceRequest.toApi(): ApiCreateSeekerExperienceRequest =
    ApiCreateSeekerExperienceRequest(
        companyName = companyName,
        position = position,
        description = description,
        startDate = startDate,
        endDate = endDate,
    )

fun ApiCreateSeekerExperienceRequest.toContract(): CreateSeekerExperienceRequest =
    CreateSeekerExperienceRequest(
        companyName = companyName,
        position = position,
        description = description,
        startDate = startDate,
        endDate = endDate,
    )

fun UpdateSeekerExperienceRequest.toApi(): ApiUpdateSeekerExperienceRequest =
    ApiUpdateSeekerExperienceRequest(
        companyName = companyName,
        position = position,
        description = description,
        startDate = startDate,
        endDate = endDate,
    )

fun ApiUpdateSeekerExperienceRequest.toContract(): UpdateSeekerExperienceRequest =
    UpdateSeekerExperienceRequest(
        companyName = companyName,
        position = position,
        description = description,
        startDate = startDate,
        endDate = endDate,
    )

fun SeekerEducationDto.toApi(): ApiSeekerEducationDto =
    ApiSeekerEducationDto(
        id = id,
        institution = institution,
        degree = degree,
        specialization = specialization,
        endYear = endYear,
    )

fun ApiSeekerEducationDto.toContract(): SeekerEducationDto =
    SeekerEducationDto(
        id = id,
        institution = institution,
        degree = degree,
        specialization = specialization,
        endYear = endYear,
    )

fun CreateSeekerEducationRequest.toApi(): ApiCreateSeekerEducationRequest =
    ApiCreateSeekerEducationRequest(
        institution = institution,
        degree = degree,
        specialization = specialization,
        endYear = endYear,
    )

fun ApiCreateSeekerEducationRequest.toContract(): CreateSeekerEducationRequest =
    CreateSeekerEducationRequest(
        institution = institution,
        degree = degree,
        specialization = specialization,
        endYear = endYear,
    )

fun UpdateSeekerEducationRequest.toApi(): ApiUpdateSeekerEducationRequest =
    ApiUpdateSeekerEducationRequest(
        institution = institution,
        degree = degree,
        specialization = specialization,
        endYear = endYear,
    )

fun ApiUpdateSeekerEducationRequest.toContract(): UpdateSeekerEducationRequest =
    UpdateSeekerEducationRequest(
        institution = institution,
        degree = degree,
        specialization = specialization,
        endYear = endYear,
    )

fun SeekerSkillsResponse.toApi(): ApiSeekerSkillsResponse =
    ApiSeekerSkillsResponse(
        skillIds = skillIds,
        skills = skills.map { it.toApi() },
    )

fun ApiSeekerSkillsResponse.toContract(): SeekerSkillsResponse =
    SeekerSkillsResponse(
        skillIds = skillIds,
        skills = skills.map { it.toContract() },
    )

fun UpdateSeekerSkillsRequest.toApi(): ApiUpdateSeekerSkillsRequest =
    ApiUpdateSeekerSkillsRequest(skillIds = skillIds)

fun ApiUpdateSeekerSkillsRequest.toContract(): UpdateSeekerSkillsRequest =
    UpdateSeekerSkillsRequest(skillIds = skillIds)

fun SeekerDesiredPositionsResponse.toApi(): ApiSeekerDesiredPositionsResponse =
    ApiSeekerDesiredPositionsResponse(
        occupationIds = occupationIds,
        occupations = occupations.map { it.toApi() },
    )

fun ApiSeekerDesiredPositionsResponse.toContract(): SeekerDesiredPositionsResponse =
    SeekerDesiredPositionsResponse(
        occupationIds = occupationIds,
        occupations = occupations.map { it.toContract() },
    )

fun UpdateSeekerDesiredPositionsRequest.toApi(): ApiUpdateSeekerDesiredPositionsRequest =
    ApiUpdateSeekerDesiredPositionsRequest(occupationIds = occupationIds)

fun ApiUpdateSeekerDesiredPositionsRequest.toContract(): UpdateSeekerDesiredPositionsRequest =
    UpdateSeekerDesiredPositionsRequest(occupationIds = occupationIds)

fun SeekerDashboardDto.toApi(): ApiSeekerDashboardDto =
    ApiSeekerDashboardDto(
        profileCompletionPercent = profileCompletionPercent,
        desiredPositionsCount = desiredPositionsCount,
        experienceCount = experienceCount,
        recommendationsPreview = recommendationsPreview.map { it.toApi() },
        testsComplete = testsComplete,
    )

fun ApiSeekerDashboardDto.toContract(): SeekerDashboardDto =
    SeekerDashboardDto(
        profileCompletionPercent = profileCompletionPercent,
        desiredPositionsCount = desiredPositionsCount,
        experienceCount = experienceCount,
        recommendationsPreview = recommendationsPreview.map { it.toContract() },
        testsComplete = testsComplete,
    )

fun SeekerPositionsOverviewDto.toApi(): ApiSeekerPositionsOverviewDto =
    ApiSeekerPositionsOverviewDto(
        occupationIds = occupationIds,
        occupations = occupations.map { it.toApi() },
        recommendations = recommendations.map { it.toApi() },
        interests = interests.toApi(),
        testsComplete = testsComplete,
    )

fun ApiSeekerPositionsOverviewDto.toContract(): SeekerPositionsOverviewDto =
    SeekerPositionsOverviewDto(
        occupationIds = occupationIds,
        occupations = occupations.map { it.toContract() },
        recommendations = recommendations.map { it.toContract() },
        interests = interests.toContract(),
        testsComplete = testsComplete,
    )

fun SurveyListItemDto.toApi(): ApiSurveyListItemDto =
    ApiSurveyListItemDto(
        id = id,
        code = code,
        name = name,
        description = description,
        status = status.toApi(),
        sortOrder = sortOrder,
        locked = locked,
    )

fun ApiSurveyListItemDto.toContract(): SurveyListItemDto =
    SurveyListItemDto(
        id = id,
        code = code,
        name = name,
        description = description,
        status = status.toContract(),
        sortOrder = sortOrder,
        locked = locked,
    )

fun SurveyGroupDto.toApi(): ApiSurveyGroupDto =
    ApiSurveyGroupDto(
        code = code,
        name = name,
        surveys = surveys.map { it.toApi() },
        completedCount = completedCount,
        totalCount = totalCount,
        status = status.toApi(),
        locked = locked,
        entrySurveyId = entrySurveyId,
    )

fun ApiSurveyGroupDto.toContract(): SurveyGroupDto =
    SurveyGroupDto(
        code = code,
        name = name,
        surveys = surveys.map { it.toContract() },
        completedCount = completedCount,
        totalCount = totalCount,
        status = status.toContract(),
        locked = locked,
        entrySurveyId = entrySurveyId,
    )

fun SurveyGroupsResponseDto.toApi(): ApiSurveyGroupsResponseDto =
    ApiSurveyGroupsResponseDto(
        groups = groups.map { it.toApi() },
        testsCompleted = testsCompleted,
        testsTotal = testsTotal,
    )

fun ApiSurveyGroupsResponseDto.toContract(): SurveyGroupsResponseDto =
    SurveyGroupsResponseDto(
        groups = groups.map { it.toContract() },
        testsCompleted = testsCompleted,
        testsTotal = testsTotal,
    )

fun SurveyDetailDto.toApi(): ApiSurveyDetailDto =
    ApiSurveyDetailDto(
        id = id,
        code = code,
        name = name,
        description = description,
        groupCode = groupCode,
        questionsJson = questionsJson,
        status = status.toApi(),
        answersJson = answersJson,
        resultId = resultId,
        locked = locked,
        stepNumber = stepNumber,
        stepTotal = stepTotal,
        prevSurveyId = prevSurveyId,
        nextSurveyId = nextSurveyId,
    )

fun ApiSurveyDetailDto.toContract(): SurveyDetailDto =
    SurveyDetailDto(
        id = id,
        code = code,
        name = name,
        description = description,
        groupCode = groupCode,
        questionsJson = questionsJson,
        status = status.toContract(),
        answersJson = answersJson,
        resultId = resultId,
        locked = locked,
        stepNumber = stepNumber,
        stepTotal = stepTotal,
        prevSurveyId = prevSurveyId,
        nextSurveyId = nextSurveyId,
    )

fun SaveSurveyAnswersRequest.toApi(): ApiSaveSurveyAnswersRequest {
    val answersObject = answers as? JsonObject ?: JsonObject(emptyMap())
    return ApiSaveSurveyAnswersRequest(answers = answersObject)
}

fun ApiSaveSurveyAnswersRequest.toContract(): SaveSurveyAnswersRequest =
    SaveSurveyAnswersRequest(answers = answers as JsonElement)

fun GlossaryTermDto.toApi(): ApiGlossaryTermDto =
    ApiGlossaryTermDto(
        id = id,
        term = term,
        definition = definition,
        description = description,
    )

fun ApiGlossaryTermDto.toContract(): GlossaryTermDto =
    GlossaryTermDto(
        id = id,
        term = term,
        definition = definition,
        description = description,
    )

fun SurveyLlmContextItemDto.toApi(): ApiSurveyLlmContextItemDto =
    ApiSurveyLlmContextItemDto(
        surveyCode = surveyCode,
        answersJson = answersJson,
        calculatedResultsJson = calculatedResultsJson,
    )

fun ApiSurveyLlmContextItemDto.toContract(): SurveyLlmContextItemDto =
    SurveyLlmContextItemDto(
        surveyCode = surveyCode,
        answersJson = answersJson,
        calculatedResultsJson = calculatedResultsJson,
    )

fun SurveyLlmContextDto.toApi(): ApiSurveyLlmContextDto =
    ApiSurveyLlmContextDto(
        surveys = surveys.map { it.toApi() },
        glossaryTerms = glossaryTerms.map { it.toApi() },
    )

fun ApiSurveyLlmContextDto.toContract(): SurveyLlmContextDto =
    SurveyLlmContextDto(
        surveys = surveys.map { it.toContract() },
        glossaryTerms = glossaryTerms.map { it.toContract() },
    )

fun CompleteSurveyResponseDto.toApi(): ApiCompleteSurveyResponseDto =
    ApiCompleteSurveyResponseDto(
        resultId = resultId,
        surveyId = surveyId,
        status = status.toApi(),
        nextSurveyId = nextSurveyId,
    )

fun ApiCompleteSurveyResponseDto.toContract(): CompleteSurveyResponseDto =
    CompleteSurveyResponseDto(
        resultId = resultId,
        surveyId = surveyId,
        status = status.toContract(),
        nextSurveyId = nextSurveyId,
    )

fun PersonalityAxesDto.toApi(): ApiPersonalityAxesDto =
    ApiPersonalityAxesDto(
        axisDominance = axisDominance,
        axisInfluence = axisInfluence,
        axisStability = axisStability,
        axisIntegrity = axisIntegrity,
        axisAutonomy = axisAutonomy,
        axisPace = axisPace,
    )

fun ApiPersonalityAxesDto.toContract(): PersonalityAxesDto =
    PersonalityAxesDto(
        axisDominance = axisDominance,
        axisInfluence = axisInfluence,
        axisStability = axisStability,
        axisIntegrity = axisIntegrity,
        axisAutonomy = axisAutonomy,
        axisPace = axisPace,
    )

fun PersonalityItemDto.toApi(): ApiPersonalityItemDto =
    ApiPersonalityItemDto(title = title, description = description)

fun ApiPersonalityItemDto.toContract(): PersonalityItemDto =
    PersonalityItemDto(title = title, description = description)

fun PersonalitySectionDto.toApi(): ApiPersonalitySectionDto =
    ApiPersonalitySectionDto(
        title = title,
        items = items.map { it.toApi() },
    )

fun ApiPersonalitySectionDto.toContract(): PersonalitySectionDto =
    PersonalitySectionDto(
        title = title,
        items = items.map { it.toContract() },
    )

fun SucceedThroughDto.toApi(): ApiSucceedThroughDto =
    ApiSucceedThroughDto(
        point0 = point0,
        point1 = point1,
        point2 = point2,
    )

fun ApiSucceedThroughDto.toContract(): SucceedThroughDto =
    SucceedThroughDto(
        point0 = point0,
        point1 = point1,
        point2 = point2,
    )

fun PersonalityTraitDetailsDto.toApi(): ApiPersonalityTraitDetailsDto =
    ApiPersonalityTraitDetailsDto(
        description = description,
        goodDay = goodDay,
        badDay = badDay,
        succeedThrough = succeedThrough.toApi(),
    )

fun ApiPersonalityTraitDetailsDto.toContract(): PersonalityTraitDetailsDto =
    PersonalityTraitDetailsDto(
        description = description,
        goodDay = goodDay,
        badDay = badDay,
        succeedThrough = succeedThrough.toContract(),
    )

fun PersonalityTraitDto.toApi(): ApiPersonalityTraitDto =
    ApiPersonalityTraitDto(
        key = key,
        label = label,
        scalePosition = scalePosition,
        leftPole = leftPole,
        rightPole = rightPole,
        details = details.toApi(),
        isTopStrength = isTopStrength,
    )

fun ApiPersonalityTraitDto.toContract(): PersonalityTraitDto =
    PersonalityTraitDto(
        key = key,
        label = label,
        scalePosition = scalePosition,
        leftPole = leftPole,
        rightPole = rightPole,
        details = details.toContract(),
        isTopStrength = isTopStrength ?: false,
    )

fun PersonalityCategoryDto.toApi(): ApiPersonalityCategoryDto =
    ApiPersonalityCategoryDto(
        key = key,
        description = description,
        topStrengthIndex = topStrengthIndex,
        traits = traits.map { it.toApi() },
    )

fun ApiPersonalityCategoryDto.toContract(): PersonalityCategoryDto =
    PersonalityCategoryDto(
        key = key,
        description = description,
        topStrengthIndex = topStrengthIndex,
        traits = traits.map { it.toContract() },
    )

fun PersonalityPreviewDto.toApi(): ApiPersonalityPreviewDto =
    ApiPersonalityPreviewDto(
        status = status.toApi(),
        generationError = generationError,
        testsCompleted = testsCompleted,
        testsTotal = testsTotal,
        title = title,
        description = description,
        profile = profile,
        autonomy = autonomy,
        thinkingStyle = thinkingStyle,
        burnoutRisk = burnoutRisk,
        axisDominance = axisDominance,
        axisInfluence = axisInfluence,
        axisStability = axisStability,
        axisIntegrity = axisIntegrity,
        axisAutonomy = axisAutonomy,
        axisPace = axisPace,
        categories = categories?.map { it.toApi() },
        energySources = energySources?.toApi(),
        stopFactors = stopFactors?.toApi(),
        superpowersAndTalents = superpowersAndTalents?.map { it.toApi() },
    )

fun ApiPersonalityPreviewDto.toContract(): PersonalityPreviewDto =
    PersonalityPreviewDto(
        status = status.toContract(),
        generationError = generationError,
        testsCompleted = testsCompleted,
        testsTotal = testsTotal,
        title = title,
        description = description,
        profile = profile,
        autonomy = autonomy,
        thinkingStyle = thinkingStyle,
        burnoutRisk = burnoutRisk,
        axisDominance = axisDominance,
        axisInfluence = axisInfluence,
        axisStability = axisStability,
        axisIntegrity = axisIntegrity,
        axisAutonomy = axisAutonomy,
        axisPace = axisPace,
        categories = categories?.map { it.toContract() },
        energySources = energySources?.toContract(),
        stopFactors = stopFactors?.toContract(),
        superpowersAndTalents = superpowersAndTalents?.map { it.toContract() },
    )

fun PersonalityProfileStatus.toGenerationStatusResponse(): ApiPersonalityGenerationStatusResponse =
    ApiPersonalityGenerationStatusResponse(status = toApi())

fun EmployerProfileDto.toApi(): ApiEmployerProfileDto =
    ApiEmployerProfileDto(
        id = id,
        name = name,
        description = description,
        website = website,
        phone = phone,
        emailContact = emailContact,
    )

fun ApiEmployerProfileDto.toContract(): EmployerProfileDto =
    EmployerProfileDto(
        id = id,
        name = name,
        description = description,
        website = website,
        phone = phone,
        emailContact = emailContact,
    )

fun UpdateEmployerProfileRequest.toApi(): ApiUpdateEmployerProfileRequest =
    ApiUpdateEmployerProfileRequest(
        name = name,
        description = description,
        website = website,
        phone = phone,
        emailContact = emailContact,
    )

fun ApiUpdateEmployerProfileRequest.toContract(): UpdateEmployerProfileRequest =
    UpdateEmployerProfileRequest(
        name = name,
        description = description,
        website = website,
        phone = phone,
        emailContact = emailContact,
    )

fun JobProfileDto.toApi(): ApiJobProfileDto =
    ApiJobProfileDto(
        id = id,
        occupationId = occupationId,
        occupationName = occupationName,
        description = description,
        isActive = isActive,
        skillIds = skillIds,
        skills = skills.map { it.toApi() },
        personalityAxes = personalityAxes.toApi(),
    )

fun ApiJobProfileDto.toContract(): JobProfileDto =
    JobProfileDto(
        id = id,
        occupationId = occupationId,
        occupationName = occupationName,
        description = description,
        isActive = isActive,
        skillIds = skillIds,
        skills = skills.map { it.toContract() },
        personalityAxes = personalityAxes.toContract(),
    )

fun CreateJobProfileRequest.toApi(): ApiCreateJobProfileRequest =
    ApiCreateJobProfileRequest(
        occupationId = occupationId,
        description = description,
        isActive = isActive,
        skillIds = skillIds,
        personalityAxes = personalityAxes?.toApi(),
    )

fun ApiCreateJobProfileRequest.toContract(): CreateJobProfileRequest =
    CreateJobProfileRequest(
        occupationId = occupationId,
        description = description,
        isActive = isActive ?: true,
        skillIds = skillIds ?: emptyList(),
        personalityAxes = personalityAxes?.toContract(),
    )

fun UpdateJobProfileRequest.toApi(): ApiUpdateJobProfileRequest =
    ApiUpdateJobProfileRequest(
        occupationId = occupationId,
        description = description,
        isActive = isActive,
        skillIds = skillIds,
        personalityAxes = personalityAxes?.toApi(),
    )

fun ApiUpdateJobProfileRequest.toContract(): UpdateJobProfileRequest =
    UpdateJobProfileRequest(
        occupationId = occupationId,
        description = description,
        isActive = isActive ?: true,
        skillIds = skillIds ?: emptyList(),
        personalityAxes = personalityAxes?.toContract(),
    )

fun EmployerDashboardDto.toApi(): ApiEmployerDashboardDto =
    ApiEmployerDashboardDto(
        companyName = companyName,
        jobProfilesCount = jobProfilesCount,
        activeJobProfilesCount = activeJobProfilesCount,
        totalMatchedCandidates = totalMatchedCandidates,
    )

fun ApiEmployerDashboardDto.toContract(): EmployerDashboardDto =
    EmployerDashboardDto(
        companyName = companyName,
        jobProfilesCount = jobProfilesCount,
        activeJobProfilesCount = activeJobProfilesCount,
        totalMatchedCandidates = totalMatchedCandidates,
    )
