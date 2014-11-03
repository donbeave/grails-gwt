# Grails GWT Plugin 
[![Build Status](https://travis-ci.org/donbeave/grails-gwt.svg?branch=master)](https://travis-ci.org/donbeave/grails-gwt)

## Introduction

The Google Web Toolkit (GWT) is an advanced AJAX framework that allows you to develop rich user interfaces in Java, thus taking advantage of type-checking and code re-use. GWT will then compile your Java code and generate fast, cross-platform Javascript that can be included in any web page you choose.
The plugin makes it easy to incorporate GWT code into your GSP pages, and it also simplifies the handling of RPC requests on the server. If you have not used GWT before, please read the documentation on the GWT website.


## Configuration

The following configuration options are available (e.g. by adding some or all of them to your `BuildConfig.groovy`):

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.version</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>
		</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.gin.version</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>
		</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.gwtp.version</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>
		</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.guava.version</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>
		</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.eventbinder.version</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>
		</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.dependencies</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>List&lt;String&gt;</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>
		</td>
	</tr>
</table>

#### Compile parameters

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.local.workers</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Integer</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>Number of parallel processes used to compile GWT premutations. Defaults to platform available processors number.</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.draft</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Boolean</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>false</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>Enable faster, but less-optimized, compilations. This is equivalent to <code>gwt.compile.optimizationLevel=0</code> plus <code>gwt.compile.aggressiveOptimization=false</code>.</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.report</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Boolean</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>false</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>Compile a report that tells the "Story of Your Compile".</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.optimizationLevel</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Integer</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>Sets the optimization level used by the compiler. <code>0</code>=none <code>9</code>=maximum. <code>-1</code> uses the default level of the compiler.</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.logLevel</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>null</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td>GWT logging level, either <code>ERROR</code>, <code>WARN</code>, <code>INFO</code>, <code>TRACE</code>, <code>DEBUG</code>, <code>SPAM</code>, or <code>ALL</code>.</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.classMetadata</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Boolean</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>true</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td><strong>EXPERIMENTAL</strong>: Disables some java.lang.Class methods (e.g. getName()).</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.castChecking</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Boolean</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>true</code></td>
	</tr>j
	<tr>
		<td>Description</td>
		<td><strong>EXPERIMENTAL</strong>: Disables run-time checking of cast operations.</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.aggressiveOptimization</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>Boolean</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>true</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td><strong>Deprecated</strong>. since 2.6.0-rc1</td>
	</tr>
</table>

<table>
	<tr>
		<td>Key</td>
		<td><strong><code>gwt.compile.jsInteropMode</code></strong></td>
	</tr>
	<tr>
		<td>Type</td>
		<td><code>String</code></td>
	</tr>
	<tr>
		<td>Default</td>
		<td><code>NONE</code></td>
	</tr>
	<tr>
		<td>Description</td>
		<td><strong>EXPERIMENTAL</strong>: Specifies JsInterop mode, either <code>NONE</code>, <code>JS</code>, or <code>CLOSURE</code>.</td>
	</tr>
</table>
