package llm;

import datatypes.Persona;
import llm.openai.OpenAIAPI;

import java.io.IOException;

public interface LargeLanguageModelAPI<T> {
    public final String CHAT_RESPONSE = "chat";
    public final String APPEARANCE_GEN_RESPONSE = "appearance_gen";
    public final String INSTRUCT_RESPONSE = "instruct";
    public String query(String responseType) throws IOException;
    public T setPersona(Persona persona);
    public T setPrompt(String prompt);
    public T setAppearenceGenPrompts(String prompt);
    public T loadAnimeRandomGenerationPrompt();

    public T setInstructPrompt(String instruction);
}
