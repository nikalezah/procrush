#!/usr/bin/env python3
"""Fix imports after server package restructure."""
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
ROOTS = [
    REPO / "server" / "src" / "main" / "kotlin",
    REPO / "server" / "src" / "test" / "kotlin",
]

REPLACEMENTS = [
    ("jobs.procrush.domain.personality.", "jobs.procrush.personality.service."),
    ("jobs.procrush.domain.seeker.", "jobs.procrush.seeker.service."),
    ("jobs.procrush.domain.employer.", "jobs.procrush.employer.service."),
    ("jobs.procrush.domain.matching.", "jobs.procrush.matching.service."),
    ("jobs.procrush.domain.ProfileProvisioningService", "jobs.procrush.auth.service.ProfileProvisioningService"),
    ("jobs.procrush.domain.PersonalityPromptBuilder", "jobs.procrush.personality.llm.PersonalityPromptBuilder"),
    ("jobs.procrush.domain.PersonalityProfileValidator", "jobs.procrush.personality.llm.PersonalityProfileValidator"),
    ("jobs.procrush.domain.PersonalityProfileMapper", "jobs.procrush.personality.llm.PersonalityProfileMapper"),
    ("jobs.procrush.domain.SurveyService", "jobs.procrush.survey.service.SurveyService"),
    ("jobs.procrush.domain.CompleteSurveyResult", "jobs.procrush.survey.service.CompleteSurveyResult"),
    ("jobs.procrush.domain.GenerationInProgressException", "jobs.procrush.shared.GenerationInProgressException"),
    ("jobs.procrush.domain.RegistrationConflictException", "jobs.procrush.shared.RegistrationConflictException"),
    ("jobs.procrush.domain.ResourceNotFoundException", "jobs.procrush.shared.ResourceNotFoundException"),
    ("jobs.procrush.domain.SurveyAlreadyCompletedException", "jobs.procrush.shared.SurveyAlreadyCompletedException"),
    ("jobs.procrush.config.", "jobs.procrush.bootstrap.config."),
    ("jobs.procrush.plugins.", "jobs.procrush.bootstrap.plugins."),
    ("jobs.procrush.di.AppContext", "jobs.procrush.bootstrap.AppContext"),
    ("jobs.procrush.db.DatabaseFactory", "jobs.procrush.bootstrap.DatabaseFactory"),
    ("jobs.procrush.db.ReferenceRepository", "jobs.procrush.shared.repository.ReferenceRepository"),
    ("jobs.procrush.db.SeekerRepository", "jobs.procrush.seeker.repository.SeekerRepository"),
    ("jobs.procrush.db.SeekerPersonalProfileRepository", "jobs.procrush.seeker.repository.SeekerPersonalProfileRepository"),
    ("jobs.procrush.db.SeekerSuperpowersAndTalentsRepository", "jobs.procrush.seeker.repository.SeekerSuperpowersAndTalentsRepository"),
    ("jobs.procrush.db.EmployerRepository", "jobs.procrush.employer.repository.EmployerRepository"),
    ("jobs.procrush.db.SurveyRepository", "jobs.procrush.survey.repository.SurveyRepository"),
    ("jobs.procrush.db.MatchingRepository", "jobs.procrush.matching.repository.MatchingRepository"),
    ("jobs.procrush.db.UserRepository", "jobs.procrush.auth.repository.UserRepository"),
    ("jobs.procrush.db.SessionRepository", "jobs.procrush.auth.repository.SessionRepository"),
    ("jobs.procrush.db.tables.OccupationsTable", "jobs.procrush.shared.tables.OccupationsTable"),
    ("jobs.procrush.db.tables.SkillsTable", "jobs.procrush.shared.tables.SkillsTable"),
    ("jobs.procrush.db.tables.GlossaryTermsTable", "jobs.procrush.shared.tables.GlossaryTermsTable"),
    ("jobs.procrush.db.tables.SuperpowersAndTalentsTable", "jobs.procrush.shared.tables.SuperpowersAndTalentsTable"),
    ("jobs.procrush.db.tables.UsersTable", "jobs.procrush.auth.tables.UsersTable"),
    ("jobs.procrush.db.tables.SessionsTable", "jobs.procrush.auth.tables.SessionsTable"),
    ("jobs.procrush.db.tables.SeekersTable", "jobs.procrush.seeker.tables.SeekersTable"),
    ("jobs.procrush.db.tables.SeekerExperienceTable", "jobs.procrush.seeker.tables.SeekerExperienceTable"),
    ("jobs.procrush.db.tables.SeekerEducationTable", "jobs.procrush.seeker.tables.SeekerEducationTable"),
    ("jobs.procrush.db.tables.SeekerSkillsTable", "jobs.procrush.seeker.tables.SeekerSkillsTable"),
    ("jobs.procrush.db.tables.SeekerDesiredPositionsTable", "jobs.procrush.seeker.tables.SeekerDesiredPositionsTable"),
    ("jobs.procrush.db.tables.SeekerPersonalProfilesTable", "jobs.procrush.seeker.tables.SeekerPersonalProfilesTable"),
    ("jobs.procrush.db.tables.SeekerSuperpowersAndTalentsTable", "jobs.procrush.seeker.tables.SeekerSuperpowersAndTalentsTable"),
    ("jobs.procrush.db.tables.EmployersTable", "jobs.procrush.employer.tables.EmployersTable"),
    ("jobs.procrush.db.tables.EmployerJobProfilesTable", "jobs.procrush.employer.tables.EmployerJobProfilesTable"),
    ("jobs.procrush.db.tables.JobProfileSkillsTable", "jobs.procrush.employer.tables.JobProfileSkillsTable"),
    ("jobs.procrush.db.tables.SurveysTable", "jobs.procrush.survey.tables.SurveysTable"),
    ("jobs.procrush.db.tables.SurveyKeysTable", "jobs.procrush.survey.tables.SurveyKeysTable"),
    ("jobs.procrush.db.tables.SurveyResultsTable", "jobs.procrush.survey.tables.SurveyResultsTable"),
    ("jobs.procrush.auth.RoleGuard", "jobs.procrush.auth.service.RoleGuard"),
    ("jobs.procrush.auth.SessionCookies", "jobs.procrush.auth.service.SessionCookies"),
    ("jobs.procrush.auth.SessionService", "jobs.procrush.auth.service.SessionService"),
    ("jobs.procrush.auth.UserAuthService", "jobs.procrush.auth.service.UserAuthService"),
    ("jobs.procrush.auth.UserProfileEnricher", "jobs.procrush.auth.service.UserProfileEnricher"),
    ("jobs.procrush.auth.SessionTokenHasher", "jobs.procrush.auth.service.SessionTokenHasher"),
    ("jobs.procrush.auth.EmailNormalizer", "jobs.procrush.auth.service.EmailNormalizer"),
    ("jobs.procrush.routes.authRoutes", "jobs.procrush.auth.route.authRoutes"),
    ("jobs.procrush.routes.referenceRoutes", "jobs.procrush.shared.route.referenceRoutes"),
    ("jobs.procrush.routes.employerRoutes", "jobs.procrush.employer.route.employerRoutes"),
    ("jobs.procrush.routes.seekerProfileRoutes", "jobs.procrush.seeker.route.seekerProfileRoutes"),
    ("jobs.procrush.routes.seekerSurveyRoutes", "jobs.procrush.survey.route.seekerSurveyRoutes"),
    ("jobs.procrush.routes.requireLongParam", "jobs.procrush.bootstrap.route.requireLongParam"),
    ("jobs.procrush.survey.SurveyFlowRules", "jobs.procrush.survey.scoring.SurveyFlowRules"),
    ("jobs.procrush.survey.SurveyStatus", "jobs.procrush.survey.dto.SurveyStatus"),
    ("jobs.procrush.survey.SurveyDefinitionDto", "jobs.procrush.survey.dto.SurveyDefinitionDto"),
    ("jobs.procrush.survey.SurveyKeyDto", "jobs.procrush.survey.dto.SurveyKeyDto"),
    ("jobs.procrush.survey.SurveyResultDto", "jobs.procrush.survey.dto.SurveyResultDto"),
    ("jobs.procrush.survey.SurveyListItemDto", "jobs.procrush.survey.dto.SurveyListItemDto"),
    ("jobs.procrush.survey.SurveyGroupDto", "jobs.procrush.survey.dto.SurveyGroupDto"),
    ("jobs.procrush.survey.SurveyGroupsResponseDto", "jobs.procrush.survey.dto.SurveyGroupsResponseDto"),
    ("jobs.procrush.survey.SurveyDetailDto", "jobs.procrush.survey.dto.SurveyDetailDto"),
    ("jobs.procrush.survey.SaveSurveyAnswersRequest", "jobs.procrush.survey.dto.SaveSurveyAnswersRequest"),
    ("jobs.procrush.survey.SurveyLlmContextDto", "jobs.procrush.survey.dto.SurveyLlmContextDto"),
    ("jobs.procrush.survey.SurveyLlmContextItemDto", "jobs.procrush.survey.dto.SurveyLlmContextItemDto"),
    ("jobs.procrush.survey.GlossaryTermDto", "jobs.procrush.survey.dto.GlossaryTermDto"),
    ("jobs.procrush.survey.CompleteSurveyResponseDto", "jobs.procrush.survey.dto.CompleteSurveyResponseDto"),
    ("jobs.procrush.models.OccupationDto", "jobs.procrush.shared.dto.OccupationDto"),
    ("jobs.procrush.models.SkillDto", "jobs.procrush.shared.dto.SkillDto"),
    ("jobs.procrush.models.SuperpowerAndTalentDto", "jobs.procrush.shared.dto.SuperpowerAndTalentDto"),
    ("jobs.procrush.models.SeekerProfileDto", "jobs.procrush.seeker.dto.SeekerProfileDto"),
    ("jobs.procrush.models.UpdateSeekerProfileRequest", "jobs.procrush.seeker.dto.UpdateSeekerProfileRequest"),
    ("jobs.procrush.models.SeekerExperienceDto", "jobs.procrush.seeker.dto.SeekerExperienceDto"),
    ("jobs.procrush.models.CreateSeekerExperienceRequest", "jobs.procrush.seeker.dto.CreateSeekerExperienceRequest"),
    ("jobs.procrush.models.UpdateSeekerExperienceRequest", "jobs.procrush.seeker.dto.UpdateSeekerExperienceRequest"),
    ("jobs.procrush.models.SeekerEducationDto", "jobs.procrush.seeker.dto.SeekerEducationDto"),
    ("jobs.procrush.models.CreateSeekerEducationRequest", "jobs.procrush.seeker.dto.CreateSeekerEducationRequest"),
    ("jobs.procrush.models.UpdateSeekerEducationRequest", "jobs.procrush.seeker.dto.UpdateSeekerEducationRequest"),
    ("jobs.procrush.models.SeekerSkillsResponse", "jobs.procrush.seeker.dto.SeekerSkillsResponse"),
    ("jobs.procrush.models.UpdateSeekerSkillsRequest", "jobs.procrush.seeker.dto.UpdateSeekerSkillsRequest"),
    ("jobs.procrush.models.SeekerDesiredPositionsResponse", "jobs.procrush.seeker.dto.SeekerDesiredPositionsResponse"),
    ("jobs.procrush.models.UpdateSeekerDesiredPositionsRequest", "jobs.procrush.seeker.dto.UpdateSeekerDesiredPositionsRequest"),
    ("jobs.procrush.models.SeekerDashboardDto", "jobs.procrush.seeker.dto.SeekerDashboardDto"),
    ("jobs.procrush.models.EmployerProfileDto", "jobs.procrush.employer.dto.EmployerProfileDto"),
    ("jobs.procrush.models.UpdateEmployerProfileRequest", "jobs.procrush.employer.dto.UpdateEmployerProfileRequest"),
    ("jobs.procrush.models.JobProfileDto", "jobs.procrush.employer.dto.JobProfileDto"),
    ("jobs.procrush.models.CreateJobProfileRequest", "jobs.procrush.employer.dto.CreateJobProfileRequest"),
    ("jobs.procrush.models.UpdateJobProfileRequest", "jobs.procrush.employer.dto.UpdateJobProfileRequest"),
    ("jobs.procrush.models.EmployerDashboardDto", "jobs.procrush.employer.dto.EmployerDashboardDto"),
    ("jobs.procrush.models.PersonalityProfileStatus", "jobs.procrush.personality.dto.PersonalityProfileStatus"),
    ("jobs.procrush.models.PersonalityTraitDetailsRules", "jobs.procrush.personality.dto.PersonalityTraitDetailsRules"),
    ("jobs.procrush.models.PersonalitySectionRules", "jobs.procrush.personality.dto.PersonalitySectionRules"),
    ("jobs.procrush.models.personalityItemWordCount", "jobs.procrush.personality.dto.personalityItemWordCount"),
    ("jobs.procrush.models.PersonalityItemDto", "jobs.procrush.personality.dto.PersonalityItemDto"),
    ("jobs.procrush.models.PersonalitySectionDto", "jobs.procrush.personality.dto.PersonalitySectionDto"),
    ("jobs.procrush.models.SuperpowerAndTalentLlmItem", "jobs.procrush.personality.dto.SuperpowerAndTalentLlmItem"),
    ("jobs.procrush.models.SeekerPersonalProfileRecord", "jobs.procrush.personality.dto.SeekerPersonalProfileRecord"),
    ("jobs.procrush.models.SeekerPersonalProfileLlmOutput", "jobs.procrush.personality.dto.SeekerPersonalProfileLlmOutput"),
    ("jobs.procrush.models.SucceedThroughDto", "jobs.procrush.personality.dto.SucceedThroughDto"),
    ("jobs.procrush.models.PersonalityTraitDetailsDto", "jobs.procrush.personality.dto.PersonalityTraitDetailsDto"),
    ("jobs.procrush.models.PersonalityTraitDto", "jobs.procrush.personality.dto.PersonalityTraitDto"),
    ("jobs.procrush.models.PersonalityCategoryDto", "jobs.procrush.personality.dto.PersonalityCategoryDto"),
    ("jobs.procrush.models.PersonalityPreviewDto", "jobs.procrush.personality.dto.PersonalityPreviewDto"),
    ("jobs.procrush.models.JobRecommendationDto", "jobs.procrush.matching.dto.JobRecommendationDto"),
    ("jobs.procrush.models.CandidateRecommendationDto", "jobs.procrush.matching.dto.CandidateRecommendationDto"),
    ("jobs.procrush.models.PersonalityAxesDto", "jobs.procrush.personality.dto.PersonalityAxesDto"),
    ("jobs.procrush.models.ConnectionsCategory", "jobs.procrush.personality.dto.ConnectionsCategory"),
    ("jobs.procrush.models.CreativityCategory", "jobs.procrush.personality.dto.CreativityCategory"),
    ("jobs.procrush.models.DriveCategory", "jobs.procrush.personality.dto.DriveCategory"),
    ("jobs.procrush.models.ThinkingCategory", "jobs.procrush.personality.dto.ThinkingCategory"),
    ("jobs.procrush.models.EnergySourcesSection", "jobs.procrush.personality.dto.EnergySourcesSection"),
    ("jobs.procrush.models.StopFactorsSection", "jobs.procrush.personality.dto.StopFactorsSection"),
    ("jobs.procrush.models.PersonalityDbJson", "jobs.procrush.personality.dto.PersonalityDbJson"),
    ("jobs.procrush.matching.repository.JobMatchCandidate", "jobs.procrush.matching.model.JobMatchCandidate"),
    ("jobs.procrush.matching.repository.SeekerMatchCandidate", "jobs.procrush.matching.model.SeekerMatchCandidate"),
    ("jobs.procrush.matching.repository.SeekerMatchingContext", "jobs.procrush.matching.model.SeekerMatchingContext"),
]

OLD_DIRS = [
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "config",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "plugins",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "di",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "routes",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "models",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "auth",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "db",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "domain",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "survey" / "SurveyModels.kt",
    REPO / "server" / "src" / "main" / "kotlin" / "jobs" / "procrush" / "survey" / "SurveyFlowRules.kt",
]


def fix_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    new = text
    for old, new_val in REPLACEMENTS:
        new = new.replace(old, new_val)
    if new != text:
        path.write_text(new, encoding="utf-8")
        return True
    return False


def main() -> None:
    changed = 0
    for root in ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*.kt"):
            if fix_file(path):
                changed += 1
                print(f"fixed: {path.relative_to(REPO)}")
    migration = REPO / "server" / "src" / "main" / "kotlin" / "db" / "migration"
    for path in migration.rglob("*.kt"):
        if fix_file(path):
            changed += 1
            print(f"fixed: {path.relative_to(REPO)}")
    import shutil
    for item in OLD_DIRS:
        if item.is_dir():
            shutil.rmtree(item, ignore_errors=True)
            print(f"removed dir: {item.relative_to(REPO)}")
        elif item.is_file():
            item.unlink(missing_ok=True)
            print(f"removed file: {item.relative_to(REPO)}")
    print(f"Done. {changed} files updated.")


if __name__ == "__main__":
    main()
