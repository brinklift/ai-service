package com.blift.aiservice.service.briefing;

import com.blift.aiservice.entity.RcicAiBriefing;
import com.blift.aiservice.entity.RcicAiContext;
import com.blift.aiservice.repository.RcicAiBriefingRepository;
import com.blift.aiservice.repository.RcicAiContextRepository;
import com.blift.aiservice.service.context.AiContextBuilderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiBriefingServiceTest {

    @Mock RcicAiContextRepository contextRepository;
    @Mock RcicAiBriefingRepository briefingRepository;
    @Mock AiContextBuilderService contextBuilderService;
    @Mock WebClient.Builder webClientBuilder;

    private AiBriefingService service;

    @BeforeEach
    void setUp() {
        service = new AiBriefingService(contextRepository, briefingRepository, contextBuilderService,
                webClientBuilder, new ObjectMapper());
        when(contextBuilderService.resolveZone(any())).thenReturn(ZoneId.of("America/Toronto"));
    }

    @Test
    void storedBriefingNeverCallsOpenAi() {
        RcicAiBriefing stored = new RcicAiBriefing();
        when(briefingRepository.findByRcicUserIdAndBriefingDate(eq(1L), any())).thenReturn(Optional.of(stored));

        assertThat(service.getOrGenerateBriefing(1L)).isSameAs(stored);
        assertThat(service.refreshBriefing(1L)).isSameAs(stored);

        verifyNoInteractions(webClientBuilder);
    }

    @Test
    void requestThatWaitedOnTheLockGetsTheFirstRequestsBriefing() {
        RcicAiBriefing stored = new RcicAiBriefing();
        when(briefingRepository.findByRcicUserIdAndBriefingDate(eq(1L), any()))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(stored));

        assertThat(service.getOrGenerateBriefing(1L)).isSameAs(stored);

        verifyNoInteractions(webClientBuilder);
    }

    @Test
    void failedContextSkipsOpenAiAndIsNotStored() {
        RcicAiContext failed = new RcicAiContext();
        failed.setBuildStatus("FAILED");
        when(briefingRepository.findByRcicUserIdAndBriefingDate(eq(1L), any())).thenReturn(Optional.empty());
        when(contextRepository.findByRcicUserIdAndContextDate(eq(1L), any())).thenReturn(Optional.empty());
        when(contextBuilderService.buildContextForRcic(eq(1L), any(), any())).thenReturn(failed);

        assertThat(service.getOrGenerateBriefing(1L).getBriefingText()).contains("temporarily unavailable");

        verifyNoInteractions(webClientBuilder);
        verify(briefingRepository, never()).save(any());
    }

    @Test
    void briefingDateIsTheRcicsLocalDay() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        when(contextBuilderService.resolveZone(1L)).thenReturn(kolkata);
        RcicAiBriefing stored = new RcicAiBriefing();
        when(briefingRepository.findByRcicUserIdAndBriefingDate(1L, LocalDate.now(kolkata))).thenReturn(Optional.of(stored));

        assertThat(service.getOrGenerateBriefing(1L)).isSameAs(stored);
    }
}
