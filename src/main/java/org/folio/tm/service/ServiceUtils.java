package org.folio.tm.service;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.folio.tm.domain.entity.base.Identifiable;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;

@UtilityClass
public class ServiceUtils {

  public static <E extends Identifiable> E initId(E identifiable) {
    if (identifiable.getId() == null) {
      identifiable.setId(UUID.randomUUID());
    }

    return identifiable;
  }

  static <E extends Identifiable> Consumer<E> initId() {
    return ServiceUtils::initId;
  }

  static <E extends Identifiable> Consumer<E> setId(UUID id) {
    return ta -> ta.setId(id);
  }

  static <E extends Identifiable> Comparator<E> comparatorById() {
    return Comparator.comparing(Identifiable::getId);
  }

  static <E extends Identifiable> List<E> mergeAndSave(List<E> incomingEntities, List<E> storedEntities,
    JpaRepository<E, UUID> repository, BiConsumer<E, E> updateDataMethod) {

    List<E> toDelete = new ArrayList<>();
    List<E> toSave = new ArrayList<>();

    merge(incomingEntities, storedEntities, comparatorById(),
      toSave::add,
      (incoming, stored) -> {
        updateDataMethod.accept(incoming, stored);
        toSave.add(stored);
      },
      toDelete::add);

    repository.flush();

    repository.deleteAllInBatch(toDelete);

    return repository.saveAllAndFlush(toSave);
  }

  @SuppressWarnings("unused")
  static <E extends Comparable<E>> void merge(Collection<E> incoming, Collection<E> stored,
    Consumer<E> addMethod, BiConsumer<E, E> updateMethod, Consumer<E> deleteMethod) {
    merge(incoming, stored, Comparable::compareTo, addMethod, updateMethod, deleteMethod);
  }

  static <E> void merge(Collection<E> incoming, Collection<E> stored, Comparator<E> comparator,
    Consumer<E> addMethod, BiConsumer<E, E> updateMethod, Consumer<E> deleteMethod) {

    var storedList = new ArrayList<>(emptyIfNull(stored));

    var incomingList = new ArrayList<>(emptyIfNull(incoming));
    incomingList.sort(comparator);

    storedList.forEach(s -> {
      int idx = Collections.binarySearch(incomingList, s, comparator);

      if (idx >= 0) { // updating
        updateMethod.accept(incomingList.get(idx), s);

        incomingList.remove(idx);
      } else { // removing
        deleteMethod.accept(s);
      }
    });

    incomingList.forEach(addMethod); // what is left in the incoming has to be inserted
  }

  @SafeVarargs
  public static <E> Example<E> example(Supplier<E> probeSupplier, Consumer<E>... probeInitializer) {
    var builder = ExampleBuilder.of(probeSupplier);

    if (probeInitializer != null) {
      for (Consumer<E> initializer : probeInitializer) {
        builder.initProbe(initializer);
      }
    }

    return builder.build();
  }

  @RequiredArgsConstructor(staticName = "of")
  private static final class ExampleBuilder<E> {

    private final Supplier<E> probeSupplier;
    private Consumer<E> probeInitializer;

    void initProbe(Consumer<E> initializer) {
      requireNonNull(initializer);

      probeInitializer = probeInitializer != null
        ? probeInitializer.andThen(initializer)
        : initializer;
    }

    Example<E> build() {
      var probe = probeSupplier.get();

      probeInitializer.accept(probe);

      return Example.of(probe);
    }
  }
}
