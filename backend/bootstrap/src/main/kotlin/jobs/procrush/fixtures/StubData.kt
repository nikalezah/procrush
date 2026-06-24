package jobs.procrush.fixtures

import jobs.procrush.personality.dto.ConnectionsCategory
import jobs.procrush.personality.dto.ConnectionsTraits
import jobs.procrush.personality.dto.CreativityCategory
import jobs.procrush.personality.dto.CreativityTraits
import jobs.procrush.personality.dto.DriveCategory
import jobs.procrush.personality.dto.DriveTraits
import jobs.procrush.personality.dto.EnergySourcesItems
import jobs.procrush.personality.dto.EnergySourcesSection
import jobs.procrush.personality.dto.PersonalityCategoryDto
import jobs.procrush.personality.dto.PersonalityItem
import jobs.procrush.personality.dto.PersonalityItemDto
import jobs.procrush.personality.dto.PersonalityPreviewDto
import jobs.procrush.personality.dto.PersonalityProfileStatus
import jobs.procrush.personality.dto.PersonalitySectionDto
import jobs.procrush.personality.dto.PersonalitySectionRules
import jobs.procrush.personality.dto.PersonalityTrait
import jobs.procrush.personality.dto.PersonalityTraitDetails
import jobs.procrush.personality.dto.PersonalityTraitDetailsDto
import jobs.procrush.personality.dto.PersonalityTraitDto
import jobs.procrush.personality.dto.SeekerPersonalProfileLlmOutput
import jobs.procrush.personality.dto.StopFactorsItems
import jobs.procrush.personality.dto.StopFactorsSection
import jobs.procrush.personality.dto.SucceedThrough
import jobs.procrush.personality.dto.SucceedThroughDto
import jobs.procrush.personality.dto.SuperpowerAndTalentLlmItem
import jobs.procrush.personality.dto.ThinkingCategory
import jobs.procrush.personality.dto.ThinkingTraits
import jobs.procrush.shared.dto.SuperpowerAndTalentDto

object PersonalityStub {
    private fun trait(
        key: String,
        label: String,
        scalePosition: Double,
        leftPole: String,
        rightPole: String,
        description: String,
        goodDay: String,
        badDay: String,
        succeedThrough: List<String>,
    ) = PersonalityTraitDto(
        key = key,
        label = label,
        scalePosition = scalePosition,
        leftPole = leftPole,
        rightPole = rightPole,
        details =
            PersonalityTraitDetailsDto(
                description = description,
                goodDay = goodDay,
                badDay = badDay,
                succeedThrough =
                    SucceedThroughDto(
                        point0 = succeedThrough[0],
                        point1 = succeedThrough[1],
                        point2 = succeedThrough[2],
                    ),
            ),
        isTopStrength = false,
    )

    private fun categoryDto(
        key: String,
        description: String,
        topStrengthIndex: Int,
        traits: List<PersonalityTraitDto>,
    ) = PersonalityCategoryDto(
        key = key,
        description = description,
        topStrengthIndex = topStrengthIndex,
        traits =
            traits.mapIndexed { index, t ->
                t.copy(key = "${key}_$index", isTopStrength = index == topStrengthIndex)
            },
    )

    private fun traitFromDto(dto: PersonalityTraitDto): PersonalityTrait {
        val st = dto.details.succeedThrough
        return PersonalityTrait(
            label = dto.label,
            scalePosition = dto.scalePosition,
            leftPole = dto.leftPole,
            rightPole = dto.rightPole,
            details =
                PersonalityTraitDetails(
                    description = dto.details.description,
                    goodDay = dto.details.goodDay,
                    badDay = dto.details.badDay,
                    succeedThrough = SucceedThrough(st.point0, st.point1, st.point2),
                ),
        )
    }

    private fun connectionsCategory(cat: PersonalityCategoryDto): ConnectionsCategory {
        val traits = cat.traits.map(::traitFromDto)
        return ConnectionsCategory(
            description = cat.description,
            topStrengthIndex = cat.topStrengthIndex,
            traits = ConnectionsTraits(traits[0], traits[1], traits[2], traits[3]),
        )
    }

    private fun creativityCategory(cat: PersonalityCategoryDto): CreativityCategory {
        val traits = cat.traits.map(::traitFromDto)
        return CreativityCategory(
            description = cat.description,
            topStrengthIndex = cat.topStrengthIndex,
            traits = CreativityTraits(traits[0], traits[1], traits[2]),
        )
    }

    private fun driveCategory(cat: PersonalityCategoryDto): DriveCategory {
        val traits = cat.traits.map(::traitFromDto)
        return DriveCategory(
            description = cat.description,
            topStrengthIndex = cat.topStrengthIndex,
            traits = DriveTraits(traits[0], traits[1], traits[2], traits[3]),
        )
    }

    private fun thinkingCategory(cat: PersonalityCategoryDto): ThinkingCategory {
        val traits = cat.traits.map(::traitFromDto)
        return ThinkingCategory(
            description = cat.description,
            topStrengthIndex = cat.topStrengthIndex,
            traits = ThinkingTraits(traits[0]),
        )
    }

    fun personalityPreview(): PersonalityPreviewDto =
        PersonalityPreviewDto(
            status = PersonalityProfileStatus.READY,
            title = "Стратегический аналитик",
            description =
                "Вы сочетаете системное мышление с прагматичным подходом к решению задач. " +
                    "Цените автономность и работаете лучше всего в среде с чёткими целями.",
            profile =
                "Вы — человек, который предпочитает глубокий анализ быстрым решениям, " +
                    "но умеет действовать решительно, когда ситуация этого требует.",
            autonomy = "Высокая потребность в самостоятельности и доверии со стороны руководства.",
            thinkingStyle = "Аналитический, системный подход с опорой на данные.",
            burnoutRisk = "Умеренный риск при хронической перегрузке и отсутствии автономии.",
            axisDominance = 0.62,
            axisInfluence = 0.45,
            axisStability = 0.71,
            axisIntegrity = 0.83,
            axisAutonomy = 0.78,
            axisPace = 0.55,
            categories =
                listOf(
                    categoryDto(
                        key = "connections",
                        description =
                            "Ваш раздел СВЯЗИ показывает, насколько хорошо вы управляете отношениями " +
                                "и насколько комфортно работаете самостоятельно.",
                        topStrengthIndex = 3,
                        traits =
                            listOf(
                                trait(
                                    key = "diplomatic_vs_direct",
                                    label = "Вы немного более дипломатичны, чем прямолинейны",
                                    scalePosition = 0.55,
                                    leftPole = "Прямолинейность",
                                    rightPole = "Дипломатичность",
                                    description =
                                        "Вы учитываете потребности других и стремитесь справедливо разрешать конфликты. " +
                                            "Слушаете собеседников и честно высказываете своё мнение.",
                                    goodDay = "Сильные социальные навыки",
                                    badDay = "Избегаете давать критическую обратную связь",
                                    succeedThrough =
                                        listOf(
                                            "умение видеть две точки зрения",
                                            "ясное изложение своей позиции",
                                            "внимательное слушание",
                                        ),
                                ),
                                trait(
                                    key = "supportive_vs_autonomous",
                                    label = "Вы немного более поддерживающи, чем автономны",
                                    scalePosition = 0.6,
                                    leftPole = "Автономность",
                                    rightPole = "Поддержка",
                                    description =
                                        "У вас есть своё мнение, но вы цените людей вокруг и хорошо слышите их точку зрения.",
                                    goodDay = "Естественно поддерживаете коллег",
                                    badDay = "Слишком сильно подстраиваетесь под чужие потребности",
                                    succeedThrough =
                                        listOf(
                                            "самостоятельность при нужности команде",
                                            "работа в интересах группы",
                                            "учёт внешних мнений",
                                        ),
                                ),
                                trait(
                                    key = "emotive_vs_balanced",
                                    label = "Вы эмоциональны",
                                    scalePosition = 0.72,
                                    leftPole = "Сбалансированность",
                                    rightPole = "Эмоциональность",
                                    description =
                                        "Вы искренне переживаете за своё дело, и это заметно в вашем отношении к работе.",
                                    goodDay = "Чувствительны к важным для вас вещам",
                                    badDay = "Слишком увлечённо относитесь к задачам",
                                    succeedThrough =
                                        listOf(
                                            "забота о том, что делаете",
                                            "полное вовлечение",
                                            "самокритичность",
                                        ),
                                ),
                                trait(
                                    key = "reserved_vs_sociable",
                                    label = "Вы сдержанны",
                                    scalePosition = 0.25,
                                    leftPole = "Сдержанность",
                                    rightPole = "Общительность",
                                    description =
                                        "Готовы заводить новые контакты при необходимости, но предпочитаете работать с знакомыми людьми " +
                                            "и комфортно чувствуете себя в одиночной работе.",
                                    goodDay = "Избегаете лишних социальных отвлечений",
                                    badDay = "Некомфортно в больших командных средах",
                                    succeedThrough =
                                        listOf(
                                            "прагматичное расширение связей",
                                            "фокус на работе",
                                            "даёте другим быть услышанными",
                                        ),
                                ),
                            ),
                    ),
                    categoryDto(
                        key = "creativity",
                        description =
                            "Ваш раздел КРЕАТИВНОСТЬ показывает, насколько оригинально и инновационно вы мыслите " +
                                "или насколько логичны и аналитичны в подходе.",
                        topStrengthIndex = 1,
                        traits =
                            listOf(
                                trait(
                                    key = "focused_vs_adaptable",
                                    label = "Вы сфокусированы",
                                    scalePosition = 0.35,
                                    leftPole = "Фокус",
                                    rightPole = "Адаптивность",
                                    description =
                                        "Чаще держите фокус на задаче или одном вопросе, чтобы найти простые практичные решения в зоне комфорта.",
                                    goodDay = "Лучше всего работаете с дедлайном",
                                    badDay = "Можете застрять в деталях",
                                    succeedThrough =
                                        listOf(
                                            "умение концентрироваться",
                                            "структурированный подход",
                                            "уважение к правилам",
                                        ),
                                ),
                                trait(
                                    key = "pragmatic_vs_innovative",
                                    label = "Вы прагматичны",
                                    scalePosition = 0.4,
                                    leftPole = "Прагматизм",
                                    rightPole = "Инновации",
                                    description =
                                        "Предпочитаете проверенные практичные решения, но остаётесь открыты к инновациям.",
                                    goodDay = "Процветаете в привычной среде",
                                    badDay = "Избегаете нестандартного мышления",
                                    succeedThrough =
                                        listOf(
                                            "практичность",
                                            "ориентация на результат",
                                            "гибкость взглядов",
                                        ),
                                ),
                                trait(
                                    key = "classical_vs_open",
                                    label = "Вы классичны в подходе",
                                    scalePosition = 0.45,
                                    leftPole = "Классика",
                                    rightPole = "Открытость опыту",
                                    description =
                                        "Цените привычное и предсказуемое, но иногда приветствуете новые идеи.",
                                    goodDay = "Уважительно сохраняете традиции",
                                    badDay = "Склонны сопротивляться изменениям",
                                    succeedThrough =
                                        listOf(
                                            "простота решений",
                                            "прагматичный подход",
                                            "надёжность",
                                        ),
                                ),
                            ),
                    ),
                    categoryDto(
                        key = "drive",
                        description =
                            "Ваш раздел ДРАЙВ показывает ваш уровень амбициозности и внутренней мотивации.",
                        topStrengthIndex = 0,
                        traits =
                            listOf(
                                trait(
                                    key = "modest_vs_confident",
                                    label = "Вы скромны",
                                    scalePosition = 0.3,
                                    leftPole = "Скромность",
                                    rightPole = "Уверенность",
                                    description =
                                        "Понимаете, что уверенность не равна компетентности, и постоянно развиваете навыки.",
                                    goodDay = "Укрепляете уверенность через компетентность",
                                    badDay = "Недостаточно продвигаете свои достижения",
                                    succeedThrough =
                                        listOf(
                                            "не принимаете успех как должное",
                                            "реалистичная оценка способностей",
                                            "работа над слабыми местами",
                                        ),
                                ),
                                trait(
                                    key = "patient_vs_achiever",
                                    label = "Вы терпеливы",
                                    scalePosition = 0.38,
                                    leftPole = "Терпение",
                                    rightPole = "Достижения",
                                    description =
                                        "Работаете усердно, но карьера не поглощает всю жизнь; даёте возможностям приходить сами.",
                                    goodDay = "Довольны тем, что имеете",
                                    badDay = "Не всегда проявляете инициативу",
                                    succeedThrough =
                                        listOf(
                                            "жизнь в моменте",
                                            "умение серьёзно относиться к делу вовремя",
                                            "учитесь на опыте других",
                                        ),
                                ),
                                trait(
                                    key = "relaxed_vs_disciplined",
                                    label = "Вы расслаблены в темпе",
                                    scalePosition = 0.42,
                                    leftPole = "Расслабленность",
                                    rightPole = "Дисциплина",
                                    description =
                                        "Любите рамки и план, но не зацикливаетесь на деталях и умеете делегировать.",
                                    goodDay = "Хорошо расставляете приоритеты",
                                    badDay = "Можете оставлять задачи незавершёнными",
                                    succeedThrough =
                                        listOf(
                                            "знание, когда взять контроль, а когда отпустить",
                                            "гибкость",
                                            "умение идти на компромисс",
                                        ),
                                ),
                                trait(
                                    key = "independent_vs_dutiful",
                                    label = "Вы исполнительны",
                                    scalePosition = 0.68,
                                    leftPole = "Независимость",
                                    rightPole = "Исполнительность",
                                    description =
                                        "Чувствуете лояльность и выполняете поручения; отзывчивы в командной работе.",
                                    goodDay = "Согласны следовать правилам и договорённостям",
                                    badDay = "Сложно сказать «нет» или оспорить авторитет",
                                    succeedThrough =
                                        listOf(
                                            "надёжность",
                                            "предсказуемость",
                                            "командная ориентация",
                                        ),
                                ),
                            ),
                    ),
                    categoryDto(
                        key = "thinking",
                        description =
                            "Ваш раздел МЫШЛЕНИЕ показывает способности, которые вы используете при решении задач, " +
                                "от интуитивного до гибкого аналитического подхода.",
                        topStrengthIndex = 0,
                        traits =
                            listOf(
                                trait(
                                    key = "intuitive_vs_agile",
                                    label = "Вы высоко гибки в мышлении",
                                    scalePosition = 0.85,
                                    leftPole = "Интуиция",
                                    rightPole = "Гибкость",
                                    description =
                                        "Быстро обучаетесь и решаете сложные задачи логически и аналитически.",
                                    goodDay = "Природный навык решения проблем",
                                    badDay = "Можете быть ограничены жаждой структуры",
                                    succeedThrough =
                                        listOf(
                                            "рациональный подход к задачам",
                                            "объективность",
                                            "постоянное обучение",
                                        ),
                                ),
                            ),
                    ),
                ),
            energySources =
                PersonalitySectionDto(
                    title = PersonalitySectionRules.ENERGY_SOURCES_TITLE,
                    items =
                        listOf(
                            PersonalityItemDto(
                                title = "Амбициозные цели и вызовы",
                                description =
                                    "Вы достигаете пика энергии, когда работаете над масштабными целями с измеримым влиянием " +
                                    "на команду и бизнес, видите прогресс и можете нести ответственность за ключевые решения " +
                                    "без лишнего контроля.",
                            ),
                            PersonalityItemDto(
                                title = "Автономность и доверие",
                                description =
                                    "Свобода выбора методов и доверие руководства заряжают вас; микроинструкции и постоянные " +
                                    "согласования, наоборот, быстро истощают мотивацию и снижают качество результата в долгосрочной " +
                                    "перспективе работы.",
                            ),
                            PersonalityItemDto(
                                title = "Смысл и развитие",
                                description =
                                    "Смысловая связь задач с миссией компании и возможность учиться новому в рабочем процессе дают " +
                                    "устойчивый приток сил даже в периоды высокой нагрузки и неопределённости на рынке.",
                            ),
                        ),
                ),
            stopFactors =
                PersonalitySectionDto(
                    title = PersonalitySectionRules.STOP_FACTORS_TITLE,
                    items =
                        listOf(
                            PersonalityItemDto(
                                title = "Микроменеджмент",
                                description =
                                    "Избыточный контроль каждого шага, отчёты ради отчётов и отсутствие полномочий принимать " +
                                    "решения подрывают вашу вовлечённость и ведут к быстрому эмоциональному выгоранию в роли.",
                            ),
                            PersonalityItemDto(
                                title = "Размытые цели",
                                description =
                                    "Размытые приоритеты, частая смена целей без объяснений и хаотичные процессы создают хронический " +
                                    "стресс и ощущение бессмысленной траты времени, которое сложно компенсировать даже высокой зарплатой.",
                            ),
                        ),
                ),
            superpowersAndTalents =
                listOf(
                    SuperpowerAndTalentDto(id = 1, name = "Стратегический лидер", isPronounced = true),
                    SuperpowerAndTalentDto(id = 3, name = "Системный анализ", isPronounced = true),
                    SuperpowerAndTalentDto(id = 4, name = "Принятие решений", isPronounced = true),
                    SuperpowerAndTalentDto(id = 6, name = "Работа с данными", isPronounced = false),
                    SuperpowerAndTalentDto(id = 8, name = "Адаптивность и обучаемость", isPronounced = false),
                ),
            testsCompleted = 1,
            testsTotal = 2,
        )

    fun personalityLlmOutput(): SeekerPersonalProfileLlmOutput {
        val preview = personalityPreview()
        val connectionsCat = preview.categories!!.first { it.key == "connections" }
        val creativityCat = preview.categories!!.first { it.key == "creativity" }
        val driveCat = preview.categories!!.first { it.key == "drive" }
        val thinkingCat = preview.categories!!.first { it.key == "thinking" }
        val energyItems = preview.energySources!!.items.map { PersonalityItem(it.title, it.description) }
        val stopItems = preview.stopFactors!!.items.map { PersonalityItem(it.title, it.description) }
        val energySourcesSection = preview.energySources!!
        val stopFactorsSection = preview.stopFactors!!
        return SeekerPersonalProfileLlmOutput(
            title = preview.title!!,
            description = preview.description!!,
            profile = preview.profile!!,
            autonomy = "Высокая потребность в самостоятельности и доверии со стороны руководства.",
            thinkingStyle = "Аналитический, системный подход с опорой на данные.",
            burnoutRisk = "Умеренный риск при хронической перегрузке и отсутствии автономии.",
            connections = connectionsCategory(connectionsCat),
            creativity = creativityCategory(creativityCat),
            drive = driveCategory(driveCat),
            thinking = thinkingCategory(thinkingCat),
            axisDominance = preview.axisDominance!!,
            axisInfluence = preview.axisInfluence!!,
            axisStability = preview.axisStability!!,
            axisIntegrity = preview.axisIntegrity!!,
            axisAutonomy = preview.axisAutonomy!!,
            axisPace = preview.axisPace!!,
            burnoutRiskOverload = 0.55,
            burnoutRiskConflicts = 0.40,
            burnoutRiskDemotivation = 0.35,
            burnoutRiskStress = 0.50,
            energySources =
                EnergySourcesSection(
                    title = energySourcesSection.title,
                    items = EnergySourcesItems(energyItems[0], energyItems[1], energyItems[2]),
                ),
            stopFactors =
                StopFactorsSection(
                    title = stopFactorsSection.title,
                    items = StopFactorsItems(stopItems[0], stopItems[1]),
                ),
            superpowersAndTalents =
                listOf(
                    SuperpowerAndTalentLlmItem(name = "Стратегический лидер", isPronounced = true),
                    SuperpowerAndTalentLlmItem(name = "Системный анализ", isPronounced = true),
                    SuperpowerAndTalentLlmItem(name = "Принятие решений", isPronounced = true),
                    SuperpowerAndTalentLlmItem(name = "Работа с данными", isPronounced = false),
                    SuperpowerAndTalentLlmItem(name = "Адаптивность и обучаемость", isPronounced = false),
                ),
        )
    }
}

