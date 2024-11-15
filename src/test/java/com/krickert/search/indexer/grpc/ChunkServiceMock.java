package com.krickert.search.indexer.grpc;

import com.github.javafaker.Faker;
import com.krickert.search.service.*;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.*;

import static java.lang.reflect.Modifier.isPublic;
import static org.mockito.Mockito.*;

public class ChunkServiceMock {

    private final Random random = new SecureRandom();
    private final List<Method> fakerMethods = new ArrayList<>();
    private final Map<Method, Object> methodCategoryMap = new HashMap<>();

    public ChunkServiceMock() {
        // List of all Faker objects with their corresponding method names
        Faker faker = new Faker();
        List<Object> fakerCategories = List.of(
                faker.ancient(), faker.app(), faker.artist(), faker.avatar(), faker.aviation(), faker.lorem(),
                faker.music(), faker.name(), faker.number(), faker.internet(), faker.phoneNumber(), faker.pokemon(),
                faker.address(), faker.business(), faker.book(), faker.chuckNorris(), faker.color(), faker.commerce(),
                faker.country(), faker.currency(), faker.company(), faker.crypto(), faker.idNumber(), faker.hacker(),
                faker.options(), faker.code(), faker.finance(), faker.food(), faker.gameOfThrones(), faker.date(),
                faker.demographic(), faker.dog(), faker.educator(), faker.elderScrolls(), faker.shakespeare(),
                faker.slackEmoji(), faker.space(), faker.superhero(), faker.bool(), faker.team(), faker.beer(),
                faker.university(), faker.cat(), faker.file(), faker.stock(), faker.lordOfTheRings(), faker.zelda(),
                faker.harryPotter(), faker.rockBand(), faker.esports(), faker.friends(), faker.hipster(), faker.job(),
                faker.twinPeaks(), faker.rickAndMorty(), faker.yoda(), faker.matz(), faker.witcher(), faker.dragonBall(),
                faker.funnyName(), faker.hitchhikersGuideToTheGalaxy(), faker.hobbit(), faker.howIMetYourMother(),
                faker.leagueOfLegends(), faker.overwatch(), faker.robin(), faker.starTrek(), faker.weather(),
                faker.lebowski(), faker.medical(), faker.animal(), faker.backToTheFuture(), faker.princessBride(),
                faker.buffy(), faker.relationships(), faker.nation(), faker.dune(), faker.aquaTeenHungerForce(),
                faker.programmingLanguage()
        );

        // Collect all public methods that return a String and take no arguments, then map them to their respective categories
        for (Object category : fakerCategories) {
            for (Method method : category.getClass().getDeclaredMethods()) {
                if (isPublic(method.getModifiers()) && method.getReturnType().equals(String.class) && method.getParameterCount() == 0) {
                    fakerMethods.add(method);
                    methodCategoryMap.put(method, category);
                }
            }
        }
    }

    public ChunkServiceGrpc.ChunkServiceBlockingStub createMock() {
        ChunkServiceGrpc.ChunkServiceBlockingStub mockStub = mock(ChunkServiceGrpc.ChunkServiceBlockingStub.class);

        // Mock chunk method
        when(mockStub.chunk(any(ChunkRequest.class))).thenAnswer(invocation -> {
            int numberOfChunks = random.nextInt(100) + 1;  // Random number from 1 to 100
            List<String> chunks = new ArrayList<>();
            List<String> lastChunkWords = new ArrayList<>();

            for (int i = 0; i < numberOfChunks; i++) {
                int numberOfWords = random.nextInt(3) + 10;  // Random number from 10 to 12
                String chunk = generateRandomWords(numberOfWords, lastChunkWords);
                chunks.add(chunk);
            }

            return ChunkReply.newBuilder().addAllChunks(chunks).build();
        });

        // Mock check (health check) method
        HealthCheckReply healthCheckReply = HealthCheckReply.newBuilder()
                .setStatus("RUNNING").setTimerRunning(7777777).setServerName("localhost-mock")
                .build();
        when(mockStub.check(any(HealthCheckRequest.class))).thenReturn(healthCheckReply);

        return mockStub;
    }

    private String generateRandomWords(int numberOfWords, List<String> lastChunkWords) throws Exception {
        StringBuilder words = new StringBuilder();
        List<String> currentWords = new ArrayList<>();

        // Add up to the last 3 words from the previous chunk
        if (!lastChunkWords.isEmpty()) {
            for (int i = 0; i < Math.min(3, lastChunkWords.size()); i++) {
                if (!words.isEmpty()) words.append(" ");
                words.append(lastChunkWords.get(i));
                currentWords.add(lastChunkWords.get(i));
            }
        }

        int wordCount = currentWords.size();
        while (wordCount < numberOfWords) {
            Method method = fakerMethods.get(random.nextInt(fakerMethods.size()));
            Object category = methodCategoryMap.get(method);
            String output = (String) method.invoke(category);

            // Tokenize the output into words
            StringTokenizer tokenizer = new StringTokenizer(output);
            while (tokenizer.hasMoreTokens() && wordCount < numberOfWords) {
                if (!words.isEmpty()) words.append(" ");
                words.append(tokenizer.nextToken());
                wordCount++;
            }
        }

        // Save the last 3 words for the next chunk
        lastChunkWords.clear();
        String[] splittedWords = words.toString().split(" ");
        lastChunkWords.addAll(Arrays.asList(splittedWords).subList(Math.max(0, splittedWords.length - 3), splittedWords.length));

        return words.toString();
    }
}