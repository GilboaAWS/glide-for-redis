/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static redis_request.RedisRequestOuterClass.RequestType.Decr;
import static redis_request.RedisRequestOuterClass.RequestType.DecrBy;
import static redis_request.RedisRequestOuterClass.RequestType.Del;
import static redis_request.RedisRequestOuterClass.RequestType.Exists;
import static redis_request.RedisRequestOuterClass.RequestType.GetString;
import static redis_request.RedisRequestOuterClass.RequestType.HashDel;
import static redis_request.RedisRequestOuterClass.RequestType.HashGet;
import static redis_request.RedisRequestOuterClass.RequestType.HashSet;
import static redis_request.RedisRequestOuterClass.RequestType.Incr;
import static redis_request.RedisRequestOuterClass.RequestType.IncrBy;
import static redis_request.RedisRequestOuterClass.RequestType.IncrByFloat;
import static redis_request.RedisRequestOuterClass.RequestType.Info;
import static redis_request.RedisRequestOuterClass.RequestType.MGet;
import static redis_request.RedisRequestOuterClass.RequestType.MSet;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.SAdd;
import static redis_request.RedisRequestOuterClass.RequestType.SCard;
import static redis_request.RedisRequestOuterClass.RequestType.SMembers;
import static redis_request.RedisRequestOuterClass.RequestType.SRem;
import static redis_request.RedisRequestOuterClass.RequestType.SetString;

import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.SetOptions;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import redis_request.RedisRequestOuterClass.Command;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;
import redis_request.RedisRequestOuterClass.RequestType;

public class TransactionTests {
    private static Stream<Arguments> getTransactionBuilders() {
        return Stream.of(Arguments.of(new Transaction()), Arguments.of(new ClusterTransaction()));
    }

    @ParameterizedTest
    @MethodSource("getTransactionBuilders")
    public void transaction_builds_protobuf_request(BaseTransaction<?> transaction) {
        List<Pair<RequestType, ArgsArray>> results = new LinkedList<>();

        transaction.get("key");
        results.add(Pair.of(GetString, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.set("key", "value");
        results.add(Pair.of(SetString, ArgsArray.newBuilder().addArgs("key").addArgs("value").build()));

        transaction.set("key", "value", SetOptions.builder().returnOldValue(true).build());
        results.add(
                Pair.of(
                        SetString,
                        ArgsArray.newBuilder()
                                .addArgs("key")
                                .addArgs("value")
                                .addArgs(RETURN_OLD_VALUE)
                                .build()));

        transaction.exists(new String[] {"key1", "key2"});
        results.add(Pair.of(Exists, ArgsArray.newBuilder().addArgs("key1").addArgs("key2").build()));

        transaction.del(new String[] {"key1", "key2"});
        results.add(Pair.of(Del, ArgsArray.newBuilder().addArgs("key1").addArgs("key2").build()));

        transaction.ping();
        results.add(Pair.of(Ping, ArgsArray.newBuilder().build()));

        transaction.ping("KING PONG");
        results.add(Pair.of(Ping, ArgsArray.newBuilder().addArgs("KING PONG").build()));

        transaction.info();
        results.add(Pair.of(Info, ArgsArray.newBuilder().build()));

        transaction.info(InfoOptions.builder().section(InfoOptions.Section.EVERYTHING).build());
        results.add(
                Pair.of(
                        Info,
                        ArgsArray.newBuilder().addArgs(InfoOptions.Section.EVERYTHING.toString()).build()));

        transaction.mset(Map.of("key", "value"));
        results.add(Pair.of(MSet, ArgsArray.newBuilder().addArgs("key").addArgs("value").build()));

        transaction.mget(new String[] {"key"});
        results.add(Pair.of(MGet, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.incr("key");
        results.add(Pair.of(Incr, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.incrBy("key", 1);
        results.add(Pair.of(IncrBy, ArgsArray.newBuilder().addArgs("key").addArgs("1").build()));

        transaction.incrByFloat("key", 2.5);
        results.add(Pair.of(IncrByFloat, ArgsArray.newBuilder().addArgs("key").addArgs("2.5").build()));

        transaction.decr("key");
        results.add(Pair.of(Decr, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.decrBy("key", 2);
        results.add(Pair.of(DecrBy, ArgsArray.newBuilder().addArgs("key").addArgs("2").build()));

        transaction.hset("key", Map.of("field", "value"));
        results.add(
                Pair.of(
                        HashSet,
                        ArgsArray.newBuilder().addArgs("key").addArgs("field").addArgs("value").build()));

        transaction.hget("key", "field");
        results.add(Pair.of(HashGet, ArgsArray.newBuilder().addArgs("key").addArgs("field").build()));

        transaction.hdel("key", new String[] {"field"});
        results.add(Pair.of(HashDel, ArgsArray.newBuilder().addArgs("key").addArgs("field").build()));

        transaction.sadd("key", new String[] {"value"});
        results.add(Pair.of(SAdd, ArgsArray.newBuilder().addArgs("key").addArgs("value").build()));

        transaction.srem("key", new String[] {"value"});
        results.add(Pair.of(SRem, ArgsArray.newBuilder().addArgs("key").addArgs("value").build()));

        transaction.smembers("key");
        results.add(Pair.of(SMembers, ArgsArray.newBuilder().addArgs("key").build()));

        transaction.scard("key");
        results.add(Pair.of(SCard, ArgsArray.newBuilder().addArgs("key").build()));

        var protobufTransaction = transaction.getProtobufTransaction().build();

        for (int idx = 0; idx < protobufTransaction.getCommandsCount(); idx++) {
            Command protobuf = protobufTransaction.getCommands(idx);

            assertEquals(results.get(idx).getLeft(), protobuf.getRequestType());
            assertEquals(
                    results.get(idx).getRight().getArgsCount(), protobuf.getArgsArray().getArgsCount());
            assertEquals(results.get(idx).getRight(), protobuf.getArgsArray());
        }
    }
}