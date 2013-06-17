Labeled LDA in Java (based on JGibbLDA)
=======================================

This is a Java implementation of Labeled LDA based on the popular
[JGibbLDA](http://jgibblda.sourceforge.net/) package. The code has been heavily
refactored and a few additional options have been added. See sections below for
more details.

Data Format
-----------

The input data format is similar to the [JGibbLDA input data
format](http://jgibblda.sourceforge.net/#_2.3._Input_Data_Format), with some
minor cosmetic changes and additional support for document labels necessary for
Labeled LDA. We first describe the (modified) input format for unlabeled
documents, followed by the (new) input format for labeled documents.

**Changed from JGibbLDA**: All input/output files must be Gzipped.

### Unlabeled Documents

Unlabeled documents have the following format:

    document_1
    document_2
    ...
    document_m

where each document is a space-separated list of terms, i.e.,:

    document_i = term_1 term_2 ... term_n

**Changed from JGibbLDA**: The first line *should not* be an integer indicating
the number of documents in the file. The original JGibbLDA code has been
modified to identify the number of documents automatically.

**Note**: Labeled and unlabeled documents may be mixed in the input file, thus
you must ensure that unlabeled documents do not begin with a left square bracket
(see Labeled Document input format below). One easy fix is to prepend a space
character (' ') to each unlabeled document line.

### Labeled Documents

Labeled documents follow a format similar to unlabeled documents, but the with
labels given at the beginning of each line and surrounded by square brackets,
e.g.:

    [label_1,1 label_1,2 ... label_1,l_1] document_1
    [label_2,1 label_2,2 ... label_2,l_2] document_2
    ...
    [label_m,1 label_m,2 ... label_m,l_m] document_m

where each label is an integer in the range [0, K-1], for K equal to the number
of topics (-ntopics).

**Note**: Labeled and unlabeled documents may be mixed in the input file. An
unlabeled document is equivalent to labeling a document with every label in the
range [0, K-1].

Usage
-----

Please see the [JGibbLDA usage](http://jgibblda.sourceforge.net/#_2.2._Command_Line_&_Input_Parameter), noting the following changes:

*   All input files must be Gzipped. All output files are also Gzipped.

*   New options have been added:

    **-nburnin <int>**: Discard this many initial iterations when taking samples.

    **-samplinglag <int>**: The number of iterations between samples.

    **-infseparately**: Inference is done separately for each document, as if
    inference for each document was performed in isolation.

    **-unlabeled**: Ignore document labels, i.e., treat every document as
    unlabeled.

*   Some options have been deleted:

    **-wordmap**: Filename is automatically built based on model path.

Contact
-------

Please direct questions to [Myle Ott](myleott@gmail.com).

License
-------

Following JGibbLDA, this code is licensed under the GPLv2. Please see the
LICENSE file for the full license.

Labeled LDA in Java
Copyright (C) 2008-2013 Myle Ott (Labeled LDA), Xuan-Hieu Phan and Cam-Tu Nguyen (JGibbLDA)

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
