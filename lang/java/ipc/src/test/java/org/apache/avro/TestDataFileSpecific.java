/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import example.avro.Bar;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;

import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestDataFileSpecific {

  @TempDir
  public File DIR;

  /*
   * Test when using SpecificDatumReader<T>() constructor to read from a file with
   * a different schema that both reader & writer schemas are found.
   */
  @Test
  void specificDatumReaderDefaultCtor() throws IOException {
    File file = new File(DIR.getPath(), "testSpecificDatumReaderDefaultCtor");

    // like the specific Foo, but with another field
    Schema s1 = new Schema.Parser()
        .parse("{\"type\":\"record\",\"name\":\"Foo\"," + "\"namespace\":\"org.apache.avro\",\"fields\":["
            + "{\"name\":\"label\",\"type\":\"string\"}," + "{\"name\":\"id\",\"type\":\"int\"}]}");

    // write a file using generic objects
    try (DataFileWriter<Record> writer = new DataFileWriter<>(new GenericDatumWriter<Record>(s1)).create(s1, file)) {
      for (int i = 0; i < 10; i++) {
        Record r = new Record(s1);
        r.put("label", "" + i);
        r.put("id", i);
        writer.append(r);
      }
    }

    // read using a 'new SpecificDatumReader<T>()' to force inference of
    // reader's schema from runtime
    try (DataFileReader<Foo> reader = new DataFileReader<>(file, new SpecificDatumReader<>())) {
      int i = 0;
      for (Foo f : reader) {
        assertEquals("" + (i++), f.getLabel());
      }
      assertEquals(10, i);
    }
  }

  @Test
  public void specificDatumReaderUnionWithLogicalType() throws IOException {
    File file = new File(DIR.getPath(), "testSpecificDatumReaderUnionWithLogicalType");
    Schema schema = Bar.SCHEMA$;

    // Create test data
    Instant value = Instant.now();
    try (DataFileWriter<Record> writer = new DataFileWriter<>(new GenericDatumWriter<Record>(schema)).create(schema,
        file)) {
      for (int i = 0; i < 10; i++) {
        Record r = new Record(schema);
        r.put("title", "title" + i);
        r.put("created_at", value.toEpochMilli() + i * 1000);
        writer.append(r);
      }
    }

    // read using a 'new SpecificDatumReader<T>()' to force inference of
    // reader's schema from runtime
    try (DataFileReader<Bar> reader = new DataFileReader<>(file, new SpecificDatumReader<>())) {
      int i = 0;
      for (Bar instance : reader) {
        assertEquals("title" + i, instance.getTitle());
        assertEquals(Instant.ofEpochMilli(value.plusSeconds(i).toEpochMilli()), instance.getCreatedAt());
        i++;
      }
      assertEquals(10, i);
    }
  }

  @Test
  public void specificDatumWriterUnionWithLogicalType() throws IOException {
    File file = new File(DIR.getPath(), "testSpecificDatumWriterUnionWithLogicalType");
    Schema schema = Bar.SCHEMA$;

    // Create test data
    Instant value = Instant.now();
    try (DataFileWriter<Bar> writer = new DataFileWriter<>(new SpecificDatumWriter<Bar>()).create(schema, file)) {
      for (int i = 0; i < 10; i++) {
        Bar r = Bar.newBuilder().setTitle("title" + i).setCreatedAt(value.plusSeconds(i)).build();
        writer.append(r);
      }
    }

    // read using a 'new SpecificDatumReader<T>()' to force inference of
    // reader's schema from runtime
    try (DataFileReader<Bar> reader = new DataFileReader<>(file, new SpecificDatumReader<>())) {
      int i = 0;
      for (Bar instance : reader) {
        assertEquals("title" + i, instance.getTitle());
        assertEquals(Instant.ofEpochMilli(value.plusSeconds(i).toEpochMilli()), instance.getCreatedAt());
        i++;
      }
      assertEquals(10, i);
    }
  }
}
