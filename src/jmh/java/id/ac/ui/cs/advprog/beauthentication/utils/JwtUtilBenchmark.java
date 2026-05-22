package id.ac.ui.cs.advprog.beauthentication.utils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class JwtUtilBenchmark {

    private static final String SECRET =
            "BenchmarkSecretKeyForJmhTestingPurposesOnly1234567890!@#$";
    private static final long EXPIRATION = 86400000L;

    private JwtUtil jwtUtilNoCache;
    private JwtUtil jwtUtilWithCache;
    private String validToken;

    @Setup(Level.Trial)
    public void setUp() {
        jwtUtilNoCache = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtilNoCache, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtilNoCache, "expiration", EXPIRATION);

        jwtUtilWithCache = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtilWithCache, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtilWithCache, "expiration", EXPIRATION);

        validToken = jwtUtilNoCache.generateToken("bench@example.com", "TITIPER", "user-001");

        jwtUtilWithCache.validateToken(validToken);
    }

    @Setup(Level.Invocation)
    public void clearCacheBeforeEachCall() {
        jwtUtilNoCache.getClaimsCache().invalidateAll();
    }

    @Benchmark
    public boolean validateToken_withoutCache() {
        return jwtUtilNoCache.validateToken(validToken);
    }

    @Benchmark
    public boolean validateToken_withCache() {
        return jwtUtilWithCache.validateToken(validToken);
    }
}
