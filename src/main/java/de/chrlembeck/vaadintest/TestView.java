package de.chrlembeck.vaadintest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Route;

@Route("test")
@org.springframework.stereotype.Component
public class TestView extends VerticalLayout {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestView.class);

	private static final long serialVersionUID = -8601771892961868928L;

	private TestRepository testRepository;

	private Grid<TestEntity> grid;

	private ExampleFilterDataProvider<TestEntity, Integer> dataProvider;

	public TestView(@Autowired TestRepository testRepository) {
		this.testRepository = Objects.requireNonNull(testRepository);
		initData();
		setSizeFull();
		grid = new Grid<>();
		grid.addColumn(TestEntity::getId).setHeader("ID").setSortable(true);
		grid.addColumn(TestEntity::getColumn1).setHeader("Column 1").setSortable(true);
		grid.addColumn(TestEntity::getColumn2).setHeader("Column 2").setSortable(true);
		grid.setHeightByRows(false);
		add(grid);


		List<QuerySortOrder> defaultSort = Collections
				.unmodifiableList(Arrays.asList(new QuerySortOrder("column1", SortDirection.ASCENDING)));
		dataProvider = new ExampleFilterDataProvider<>(testRepository, null,
				defaultSort);
		grid.setDataProvider(dataProvider);
	}

	private void initData() {
		List<TestEntity> entities = new ArrayList<>();
		for (int i = 0; i < 26 * 10; i++) {
			TestEntity entity = new TestEntity("" + (char) ('A' + i / 10) + i % 10,
					"" + (char) ('Z' - (i % 26)) + i / 26);
			entities.add(entity);
		}
		testRepository.saveAll(entities);
	}
}