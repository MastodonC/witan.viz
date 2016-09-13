# witan-viz

A ClojureScript application for visualising data.

## Query Parameters
### data
**Format** `<key> / <label>:<key>`  
**Info** Data without a label will automatically be given an the index in which it appears in the query as a label. You can also specify a direct link, rather than a datastore ID.  
**Example** `?data=foo::20160822T125438Z/population.csv`
`?data=http://my.server/mydata/population.csv`
### style
**Format** `<style>`  
**Info** Supported types: table, lineplot.  
**Example** `?style=lineplot`
### spinner
**Format** `true`|`false`  
**Info** Defaults to `true`. If `true` displays a spinner during load.  
**Example** `?spinner=false`
### filter
**Format** `[<label>:]<column><operation><variable>[, multiple]`  
**Info** Filters to perform on the data once downloaded. Label to apply to a specific dataset. Comma separated for multiple columns.  
**Example** `?filter=age%3D10,foo::sex%3DM` (age=10 (all datasets) sex=M (foo dataset))

## Style-specific Parameters
**Format** `args[<name>]=<value>`  
**Info** Arguments specific for the style  
**Example** `?args[legend]=true&args[x]=popn`
### Table
**Argument** dataset  
**Info** Select a dataset *by label* to show in the table. If not found, first dataset will be selected.
### Line Plot
**Argument** x  
**Info** Column to use for X axis  
**Argument** y  
**Info** Column to use for Y axis

## Development Mode

The easiest way to develop is to have a CORS-enabled local webserver serve some CSV files for you. I use [this gist](https://gist.github.com/enjalot/2904124) but there are other command-line tools that will work just as well. As an example, once I have both figwheel and the local webserver enabled, I can access a CSV with: `http://localhost:3448/?data=http://localhost:8000population.csv&style=table`

### Compile css:

Compile css file once.

```
lein garden once
```

Automatically recompile css file on change.

```
lein garden auto
```

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3448](http://localhost:3448).

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
