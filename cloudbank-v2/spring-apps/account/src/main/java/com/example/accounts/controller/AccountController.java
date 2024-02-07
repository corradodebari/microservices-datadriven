// Copyright (c) 2023,2024 Oracle and/or its affiliates. 
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/ 

package com.example.accounts.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accounts.model.Account;
import com.example.accounts.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

import org.springframework.web.bind.annotation.DeleteMapping;

//Select AI import

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.sql.Connection;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;



@RestController
@RequestMapping("/api/v1")
public class AccountController {

    final AccountRepository accountRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/hello")
    public String ping() {
        return "Hello from Spring Boot";
    }

    @GetMapping("/accounts")
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @PostMapping("/account")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        try {
            Account _account = accountRepository.saveAndFlush(account);
            return new ResponseEntity<>(_account, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable("accountId") long accountId) {
        Optional<Account> accountData = accountRepository.findById(accountId);
        try {
            return accountData.map(account -> new ResponseEntity<>(account, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/account/getAccounts/{customerId}")
    public ResponseEntity<List<Account>> getAccountsByCustomerId(@PathVariable("customerId") String customerId) {
        try {
            List<Account> accountData = new ArrayList<Account>();
            accountData.addAll(accountRepository.findByAccountCustomerId(customerId));
            if (accountData.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(accountData, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/account/{accountId}")
    public ResponseEntity<HttpStatus> deleteAccount(@PathVariable("accountId") long accountId) {
        try {
            accountRepository.deleteById(accountId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //Helper functions to Select AI interaction

    private String getNarrate(EntityManager entityManager, String question) {
        String sqlText = "select ai narrate " + question;
        final String[] sqlRet = new String[1];

        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlText)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        sqlRet[0] = resultSet.getString(1);
                        System.out.println(sqlRet[0]); 

                    } else {
                        System.out.println("The result set is empty.");
                    }
                }
            }
        });
        return sqlRet[0];

    }

    private String showsql(EntityManager entityManager, String question) {
        String sqlText = "select ai showsql " + question;
        final String[] sqlRet = new String[1];

        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlText)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        sqlRet[0] = resultSet.getString(1);
                        System.out.println(sqlRet[0]); 

                    } else {
                        System.out.println("The result set is empty.");
                    }
                }
            }
        });
        return sqlRet[0];

    }

    private String convertJsonToList(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder result = new StringBuilder();
        try {
            List<Map<String, Object>> records = mapper.readValue(jsonString,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            for (Map<String, Object> record : records) {
                result.append("Record:\n");
                record.forEach((key, value) -> result.append(key).append(": ").append(value).append("\n"));
                result.append("\n"); 
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    private String runSQl(EntityManager entityManager, String query) {
        ObjectMapper mapper = new ObjectMapper();
        Session session = entityManager.unwrap(Session.class);
        final ArrayNode resultArray = mapper.createArrayNode();

        session.doWork(connection -> {
            executeQueryAndConvertToJson(connection, query, resultArray);
        });

        return resultArray.toPrettyString();
    }

    private void executeQueryAndConvertToJson(Connection connection, String query, ArrayNode resultArray) {
        ObjectMapper mapper = new ObjectMapper();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                ResultSet resultSet = preparedStatement.executeQuery()) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columnNames = IntStream.rangeClosed(1, columnCount)
                    .mapToObj(i -> {
                        try {
                            return metaData.getColumnName(i);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            while (resultSet.next()) {
                ObjectNode rowNode = mapper.createObjectNode();
                for (String columnName : columnNames) {
                    rowNode.put(columnName, resultSet.getString(columnName));
                }
                resultArray.add(rowNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Service for Select AI
    @PostMapping("/account-chat/{type}")
    @Transactional
    public ResponseEntity<String> chatAccount(@PathVariable("type") String responseType, @RequestBody String question) {

        // Set profile
        entityManager.createNativeQuery("BEGIN dbms_cloud_ai.set_profile(profile_name => 'openai_gpt35'); END;")
                .executeUpdate();

        System.out.println(responseType);

        String json = "";
        String narrate = "";
        String sqlText = "";
        String plainText = "";
        question = question.replaceAll("[;?!\"\']", " ");
        try {
            // GET SQL
            sqlText = showsql(entityManager, question);
            System.out.println("sqlText: " + sqlText);
            // RUNSQL
            if ("runsql".equals(responseType)) {
                json = runSQl(entityManager, sqlText);

                plainText = convertJsonToList(json);
                return new ResponseEntity<>(plainText, HttpStatus.OK);
                // NARRATE
            } else if ("narrate".equals(responseType)) {
                narrate = getNarrate(entityManager, question);
                String jsonSingleResponse = new ObjectMapper()
                        .writeValueAsString(Collections.singletonMap("answer", narrate));
                return new ResponseEntity<>(jsonSingleResponse, HttpStatus.OK);
                // SHOWSQL
            } else if ("showsql".equals(responseType)) {
                return new ResponseEntity<>("{\"answer\":\"" + sqlText + "\"}", HttpStatus.OK);
            } else {
                String error_message = "Wrong response type";
                String jsonResponse = "{\"ERROR\":\"" + error_message + "\"}";
                return new ResponseEntity<>(jsonResponse, HttpStatus.NOT_ACCEPTABLE);
            }
        } catch (Exception ex) {
            String jsonResponse = "{\"ERROR\":\"" + ex.toString() + "\"}";
            return new ResponseEntity<>(jsonResponse, HttpStatus.NOT_ACCEPTABLE);
        }
    }


}
