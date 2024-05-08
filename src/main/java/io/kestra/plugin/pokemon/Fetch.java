package io.kestra.plugin.pokemon;

import io.kestra.core.models.annotations.Plugin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.*;
import java.net.*;
import java.util.*;
import io.kestra.plugin.pokemon.Pokemon;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Fetch the pokemon details.",
    description = "Fetches all the details about the given pokemon."
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Fetching the details for pikachu",
            code = {
                "pokemon: gengar"
            }
        )
    }
)
public class Fetch extends Task implements RunnableTask<Fetch.Output> {
    @Schema(
        title = "Name of the pokemon.",
        description = "Name of the pokemon for which details need to be fetched."
    )
    @PluginProperty(dynamic = true) // If the variables will be rendered with template {{ }}
    @Builder.Default
    private String pokemon = "pikachu";

    @Override
    public Fetch.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String inputPokemon = runContext.render(pokemon);
        //logger.debug(render);
        StringBuilder result = new StringBuilder();
        URL url = new URL("https://pokeapi.co/api/v2/pokemon/" + inputPokemon);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }

        Pokemon pokemonObject = om.readValue(result.toString(), Pokemon.class);
        List<String> abilities = new ArrayList();
        for(DetailedAbility detailedAbility: pokemonObject.abilities) {
            abilities.add(detailedAbility.ability.name);
        }
        List<String> moves = new ArrayList();
        for(DetailedMove detailedMove: pokemonObject.moves) {
            moves.add(detailedMove.move.name);
        }

        return Output.builder()
            .abilities(abilities)
            .baseExperience(pokemonObject.base_experience)
            .height(pokemonObject.height)
            .moves(moves)
            .build();
    }

    /**
     * Input or Output can be nested as you need
     */
    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Abilities of the pokemon."
        )
        private final List<String> abilities;
        @Schema(
            title = "Whether the ability is hidden."
        )
        private final long baseExperience;
        @Schema(
            title = "Slot corresponding to the ability."
        )
        private final long height;
        @Schema(
            title = "Slot corresponding to the ability."
        )
        private final List<String> moves;
    }
}
