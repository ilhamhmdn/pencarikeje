package com.kejelah.pencarikeje.jobimport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobPostingExtractorTest {

    private final JobPostingExtractor extractor = new JobPostingExtractor();

    @Test
    void extractsFromABareJobPostingObject() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org/",
                  "@type": "JobPosting",
                  "title": "Backend Engineer",
                  "hiringOrganization": { "@type": "Organization", "name": "Acme Corp" },
                  "description": "<p>Build things.</p><ul><li>Java</li><li>Postgres</li></ul>"
                }
                </script>
                </head><body></body></html>
                """;

        JobPostingExtractor.Extracted result = extractor.extract(html);

        assertThat(result.companyName()).isEqualTo("Acme Corp");
        assertThat(result.roleName()).isEqualTo("Backend Engineer");
        assertThat(result.jobDescription()).contains("Build things.").contains("Java").contains("Postgres");
    }

    @Test
    void extractsFromAGraphWrappedBlock() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org/",
                  "@graph": [
                    { "@type": "WebPage", "name": "irrelevant" },
                    {
                      "@type": ["JobPosting"],
                      "title": "Frontend Engineer",
                      "hiringOrganization": "Beta LLC",
                      "description": "Plain text description."
                    }
                  ]
                }
                </script>
                </head><body></body></html>
                """;

        JobPostingExtractor.Extracted result = extractor.extract(html);

        assertThat(result.companyName()).isEqualTo("Beta LLC");
        assertThat(result.roleName()).isEqualTo("Frontend Engineer");
        assertThat(result.jobDescription()).isEqualTo("Plain text description.");
    }

    @Test
    void returnsAllNullsWhenNoJobPostingBlockExists() {
        String html = """
                <html><head>
                <script type="application/ld+json">{ "@type": "Organization", "name": "Acme Corp" }</script>
                </head><body><p>Just a regular page.</p></body></html>
                """;

        JobPostingExtractor.Extracted result = extractor.extract(html);

        assertThat(result.companyName()).isNull();
        assertThat(result.roleName()).isNull();
        assertThat(result.jobDescription()).isNull();
    }

    @Test
    void returnsAllNullsForMalformedJsonWithoutThrowing() {
        String html = """
                <html><head>
                <script type="application/ld+json">{ this is not valid json </script>
                </head><body></body></html>
                """;

        JobPostingExtractor.Extracted result = extractor.extract(html);

        assertThat(result.companyName()).isNull();
        assertThat(result.roleName()).isNull();
        assertThat(result.jobDescription()).isNull();
    }
}
