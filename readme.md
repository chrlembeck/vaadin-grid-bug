## Sample application for reproducing a bug in the vaadin 10 (and 11) Grid.

The bug is reported at the vaadin bug board as [Issue 12213](https://github.com/vaadin/framework/issues/11213) 

The used grid will be filled by an DataPovider and uses pagination. If the grid ist sorted by "column 1" in ascending order, the grid works fine. Sorting the grid by "column 2" or even the id in descending order and scrolling up and down leads to absolutely unsorted and unexpected results.

The reason for the unexpeced results lies in the fact, that the grid does not give the information about its sort columns and their direction to the underlying DataProvider. The Query-Object, that is passed to the Dataprovider in its fetch-method is always an empty array. I think it should contain informations about the columns, the table should be sorted by.

### How to reproduce?
Run the application by executing 

    mvn spring-boot:run
    
Then open the application in your browser at location

    http://localhost:test
    
Scroll a little but up and down, sort the table by column 2 or id and scroll again. At least, if the table is sorted by any of the columns in descending order, the items will be displayed totally unsorted.   
