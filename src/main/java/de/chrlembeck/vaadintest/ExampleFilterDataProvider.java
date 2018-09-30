package de.chrlembeck.vaadintest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.DataProviderListener;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

public class ExampleFilterDataProvider<T, ID extends Serializable> implements ConfigurableFilterDataProvider<T, T, T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExampleFilterDataProvider.class);

	private static final long serialVersionUID = 3806373528870321984L;

	private final JpaRepository<T, ID> repository;

	private final ExampleMatcher matcher;

	private final List<QuerySortOrder> defaultSort;

	private final ConfigurableFilterDataProvider<T, T, T> delegate;

	public ExampleFilterDataProvider(JpaRepository<T, ID> repository,
			ExampleMatcher matcher,
			List<QuerySortOrder> defaultSort) {
		if (Objects.requireNonNull(defaultSort).isEmpty()) {
			throw new IllegalArgumentException("At least one sort property must be specified!");
		}
		this.repository = repository;
		this.matcher = matcher;
		this.defaultSort = defaultSort;

		delegate = buildDataProvider();
	}

	private ConfigurableFilterDataProvider<T, T, T> buildDataProvider() {

		FetchCallback<T, T> fetchCallback = q -> {
			LOGGER.debug("*** offset=" + q.getOffset() + ", limit=" + q.getLimit() + ", sortOrders=" + q.getSortOrders()
					+ ", inMemorySorting=" + q.getInMemorySorting());

			List<T> entities = q.getFilter()
					.map(document -> repository.findAll(buildExample(document), ChunkRequest.of(q, defaultSort))
							.getContent())
					.orElseGet(() -> repository.findAll(ChunkRequest.of(q, defaultSort)).getContent());
			Comparator<T> inMemorySorting = q.getInMemorySorting();
			if (inMemorySorting != null) {
				entities = new ArrayList<>(entities);
				entities.sort(inMemorySorting);
			}
			return entities.stream();
		};
		CountCallback<T, T> countCallback = q -> q.getFilter()
				.map(document -> repository.count(buildExample(document)))
				.orElseGet(repository::count).intValue();
		CallbackDataProvider<T, T> dataProvider = DataProvider.fromFilteringCallbacks(fetchCallback, countCallback);

		return dataProvider.withConfigurableFilter((q, c) -> c);
	}

	private Example<T> buildExample(T probe) {
		return Example.of(probe, matcher);
	}

	@Override
	public void setFilter(T filter) {
		delegate.setFilter(filter);
	}

	@Override
	public boolean isInMemory() {
		return delegate.isInMemory();
	}

	@Override
	public int size(Query<T, T> query) {
		return delegate.size(query);
	}

	@Override
	public Stream<T> fetch(Query<T, T> query) {
		return delegate.fetch(query);
	}

	@Override
	public void refreshItem(T item) {
		delegate.refreshItem(item);
	}

	@Override
	public void refreshAll() {
		delegate.refreshAll();
	}

	@Override
	public Object getId(T item) {
		return delegate.getId(item);
	}

	@Override
	public Registration addDataProviderListener(DataProviderListener<T> listener) {
		return delegate.addDataProviderListener(listener);
	}

	@Override
	public <C> DataProvider<T, C> withConvertedFilter(SerializableFunction<C, T> filterConverter) {
		return delegate.withConvertedFilter(filterConverter);
	}

	@Override
	public <Q, C> ConfigurableFilterDataProvider<T, Q, C> withConfigurableFilter(
			SerializableBiFunction<Q, C, T> filterCombiner) {
		return delegate.withConfigurableFilter(filterCombiner);
	}

	@Override
	public ConfigurableFilterDataProvider<T, Void, T> withConfigurableFilter() {
		return delegate.withConfigurableFilter();
	}

	private static class ChunkRequest implements Pageable {

		public static <T> ChunkRequest of(Query<T, T> q, List<QuerySortOrder> defaultSort) {
			return new ChunkRequest(q.getOffset(), q.getLimit(), mapSort(q.getSortOrders(), defaultSort));
		}

		private static Sort mapSort(List<QuerySortOrder> sortOrders, List<QuerySortOrder> defaultSort) {
			LOGGER.debug("mapSort: sortOrders=" + sortOrders + ", defaultSortOrder=" + defaultSort);
			if (sortOrders == null || sortOrders.isEmpty()) {
				return Sort.by(mapSortCriteria(defaultSort));
			} else {
				return Sort.by(mapSortCriteria(sortOrders));
			}
		}

		private static Sort.Order[] mapSortCriteria(List<QuerySortOrder> sortOrders) {
			return sortOrders.stream()
					.map(s -> new Sort.Order(
							s.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC,
							s.getSorted()))
					.toArray(Sort.Order[]::new);
		}

		private final Sort sort;

		private int limit = 0;

		private long offset = 0;

		private ChunkRequest(long offset, int limit, Sort sort) {
			LOGGER.debug("new chunk request: offset=" + offset + ", limit=" + limit + ", sort=" + sort);
			if (offset < 0) {
				throw new IllegalArgumentException("Negative offset. " + offset);
			}
			if (limit <= 0) {
				throw new IllegalArgumentException("Limit to small. " + limit);
			}
			this.sort = sort;
			this.offset = offset;
			this.limit = limit;
		}

		@Override
		public int getPageNumber() {
			return 0;
		}

		@Override
		public int getPageSize() {
			return limit;
		}

		@Override
		public long getOffset() {
			return offset;
		}

		@Override
		public Sort getSort() {
			return sort;
		}

		@Override
		public Pageable next() {
			return null;
		}

		@Override
		public Pageable previousOrFirst() {
			return this;
		}

		@Override
		public Pageable first() {
			return this;
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}
	}
}