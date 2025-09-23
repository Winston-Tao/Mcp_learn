package com.example.mcp.prompts;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class TemplatePromptProvider extends AbstractPromptProvider {

    private static final Map<String, PromptTemplate> PROMPTS = Map.of(
        "code_review", new PromptTemplate(
            "code_review",
            "Generate a code review prompt for given code",
            List.of(
                Map.of("name", "code", "description", "The code to review", "type", "string", "required", true),
                Map.of("name", "language", "description", "Programming language", "type", "string", "required", false),
                Map.of("name", "focus", "description", "Review focus areas", "type", "string", "required", false)
            )
        ),
        "documentation", new PromptTemplate(
            "documentation",
            "Generate documentation prompt for code or API",
            List.of(
                Map.of("name", "code", "description", "Code to document", "type", "string", "required", true),
                Map.of("name", "style", "description", "Documentation style", "type", "string", "required", false),
                Map.of("name", "audience", "description", "Target audience", "type", "string", "required", false)
            )
        ),
        "debug_assistant", new PromptTemplate(
            "debug_assistant",
            "Generate debugging assistance prompt",
            List.of(
                Map.of("name", "error", "description", "Error message or description", "type", "string", "required", true),
                Map.of("name", "context", "description", "Code context or stack trace", "type", "string", "required", false),
                Map.of("name", "language", "description", "Programming language", "type", "string", "required", false)
            )
        ),
        "meeting_summary", new PromptTemplate(
            "meeting_summary",
            "Generate meeting summary prompt",
            List.of(
                Map.of("name", "transcript", "description", "Meeting transcript or notes", "type", "string", "required", true),
                Map.of("name", "participants", "description", "Meeting participants", "type", "string", "required", false),
                Map.of("name", "focus", "description", "Summary focus areas", "type", "string", "required", false)
            )
        ),
        "creative_writing", new PromptTemplate(
            "creative_writing",
            "Generate creative writing prompt",
            List.of(
                Map.of("name", "genre", "description", "Writing genre", "type", "string", "required", false),
                Map.of("name", "theme", "description", "Theme or topic", "type", "string", "required", false),
                Map.of("name", "length", "description", "Target length", "type", "string", "required", false)
            )
        )
    );

    @Override
    public List<Map<String, Object>> listPrompts() {
        return PROMPTS.values().stream()
            .map(template -> createPromptInfo(
                template.name,
                template.description,
                template.arguments
            ))
            .toList();
    }

    @Override
    public boolean hasPrompt(String name) {
        return PROMPTS.containsKey(name);
    }

    @Override
    public Mono<Map<String, Object>> getPrompt(String name, Map<String, Object> arguments) {
        return Mono.fromCallable(() -> {
            PromptTemplate template = PROMPTS.get(name);
            if (template == null) {
                throw new IllegalArgumentException("Unknown prompt: " + name);
            }

            return switch (name) {
                case "code_review" -> generateCodeReviewPrompt(arguments);
                case "documentation" -> generateDocumentationPrompt(arguments);
                case "debug_assistant" -> generateDebugAssistantPrompt(arguments);
                case "meeting_summary" -> generateMeetingSummaryPrompt(arguments);
                case "creative_writing" -> generateCreativeWritingPrompt(arguments);
                default -> throw new IllegalArgumentException("Unsupported prompt: " + name);
            };
        });
    }

    private Map<String, Object> generateCodeReviewPrompt(Map<String, Object> arguments) {
        String code = (String) arguments.get("code");
        String language = (String) arguments.getOrDefault("language", "unknown");
        String focus = (String) arguments.getOrDefault("focus", "general code quality, security, and best practices");

        String systemPrompt = String.format("""
            You are an expert code reviewer with extensive experience in %s and software engineering best practices.

            Focus your review on: %s

            Please provide:
            1. Overall assessment of code quality
            2. Specific issues or improvements
            3. Security considerations
            4. Performance implications
            5. Best practice recommendations

            Be constructive and educational in your feedback.
            """, language, focus);

        String userPrompt = String.format("""
            Please review the following %s code:

            ```%s
            %s
            ```

            Provide a detailed code review covering the focus areas mentioned in the system prompt.
            """, language, language, code);

        List<Map<String, Object>> messages = List.of(
            createTextMessage("system", systemPrompt),
            createTextMessage("user", userPrompt)
        );

        return createPromptResponse("Code review prompt for " + language + " code", messages);
    }

    private Map<String, Object> generateDocumentationPrompt(Map<String, Object> arguments) {
        String code = (String) arguments.get("code");
        String style = (String) arguments.getOrDefault("style", "technical documentation");
        String audience = (String) arguments.getOrDefault("audience", "developers");

        String systemPrompt = String.format("""
            You are a technical writer specializing in %s for %s.

            Create clear, comprehensive documentation that includes:
            1. Purpose and overview
            2. Parameters and return values
            3. Usage examples
            4. Edge cases and limitations
            5. Related functions or concepts

            Write in a style appropriate for %s.
            """, style, audience, audience);

        String userPrompt = String.format("""
            Please create documentation for the following code:

            ```
            %s
            ```

            Target audience: %s
            Documentation style: %s
            """, code, audience, style);

        List<Map<String, Object>> messages = List.of(
            createTextMessage("system", systemPrompt),
            createTextMessage("user", userPrompt)
        );

        return createPromptResponse("Documentation prompt for " + audience, messages);
    }

    private Map<String, Object> generateDebugAssistantPrompt(Map<String, Object> arguments) {
        String error = (String) arguments.get("error");
        String context = (String) arguments.getOrDefault("context", "No additional context provided");
        String language = (String) arguments.getOrDefault("language", "unknown");

        String systemPrompt = String.format("""
            You are a debugging expert with deep knowledge of %s and common programming issues.

            Help analyze and resolve the reported error by:
            1. Identifying the root cause
            2. Explaining why the error occurs
            3. Providing step-by-step solution
            4. Suggesting prevention strategies
            5. Offering alternative approaches if applicable

            Be thorough but practical in your guidance.
            """, language);

        String userPrompt = String.format("""
            I'm encountering the following error in my %s code:

            Error: %s

            Additional context:
            %s

            Please help me understand and resolve this issue.
            """, language, error, context);

        List<Map<String, Object>> messages = List.of(
            createTextMessage("system", systemPrompt),
            createTextMessage("user", userPrompt)
        );

        return createPromptResponse("Debug assistance for " + language + " error", messages);
    }

    private Map<String, Object> generateMeetingSummaryPrompt(Map<String, Object> arguments) {
        String transcript = (String) arguments.get("transcript");
        String participants = (String) arguments.getOrDefault("participants", "Meeting participants");
        String focus = (String) arguments.getOrDefault("focus", "key decisions, action items, and next steps");

        String systemPrompt = String.format("""
            You are a professional meeting facilitator and note-taker.

            Create a comprehensive meeting summary focusing on: %s

            Include:
            1. Meeting overview and objectives
            2. Key discussion points
            3. Decisions made
            4. Action items with owners and deadlines
            5. Next steps and follow-up requirements

            Present information clearly and actionably.
            """, focus);

        String userPrompt = String.format("""
            Please summarize the following meeting:

            Participants: %s

            Meeting transcript/notes:
            %s

            Focus areas: %s
            """, participants, transcript, focus);

        List<Map<String, Object>> messages = List.of(
            createTextMessage("system", systemPrompt),
            createTextMessage("user", userPrompt)
        );

        return createPromptResponse("Meeting summary focusing on " + focus, messages);
    }

    private Map<String, Object> generateCreativeWritingPrompt(Map<String, Object> arguments) {
        String genre = (String) arguments.getOrDefault("genre", "fiction");
        String theme = (String) arguments.getOrDefault("theme", "human connection");
        String length = (String) arguments.getOrDefault("length", "short story");

        String systemPrompt = String.format("""
            You are a creative writing coach and accomplished author in the %s genre.

            Create an engaging writing prompt that:
            1. Inspires creativity and imagination
            2. Provides specific but flexible direction
            3. Includes character, setting, or conflict elements
            4. Encourages exploration of %s themes
            5. Suits a %s format

            Make the prompt intriguing and open-ended.
            """, genre, theme, length);

        String userPrompt = String.format("""
            Please create a creative writing prompt with these specifications:

            Genre: %s
            Theme: %s
            Target length: %s

            Include specific elements to spark creativity while allowing for personal interpretation.
            """, genre, theme, length);

        List<Map<String, Object>> messages = List.of(
            createTextMessage("system", systemPrompt),
            createTextMessage("user", userPrompt)
        );

        return createPromptResponse("Creative writing prompt for " + genre + " " + length, messages);
    }

    private static class PromptTemplate {
        final String name;
        final String description;
        final List<Map<String, Object>> arguments;

        PromptTemplate(String name, String description, List<Map<String, Object>> arguments) {
            this.name = name;
            this.description = description;
            this.arguments = arguments;
        }
    }
}