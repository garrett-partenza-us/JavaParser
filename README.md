# Mini Parser Coded From Scratch in Java

##### This a parser written in Java that reads and parses programs writtin in the mini programming language outlined below. The output of the parser is the WebGraphViz code to display the visual graph at Webgraphviz.com. This grammar is shown grammar.pdf file.

##### *An example of a valid program*

> abs := n  
if n < 0 then abs := 0 - abs fi  
sum := 0  
read count  
while count > 0 do  
 read n  
 sum := sum + n  
 count := count - 1  
od  
write sum  

##### To run, cd into master and execute the following commands
```
rm -r p1
javac -d . MAIN_Compiler.java parser.java 
java p1.MAIN_Compiler ./input.txt 
```


