package com.iambadatplaying.modules.accounts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.object.RSOAuthenticationManager;
import com.iambadatplaying.modules.BasicConfig;
import com.iambadatplaying.modules.BasicServlet;
import com.iambadatplaying.modules.accounts.structs.Account;
import com.iambadatplaying.modules.accounts.structs.AccountList;
import com.iambadatplaying.modules.accounts.structs.accessMap.InvalidAccessException;
import com.iambadatplaying.modules.accounts.structs.accessMap.ValidatingAccessMap;
import com.iambadatplaying.modules.accounts.structs.rest.AddAccountBody;
import com.iambadatplaying.modules.accounts.structs.rest.AddAccountListBody;
import com.iambadatplaying.modules.accounts.structs.rest.UpdateAccountListBody;
import com.iambadatplaying.modules.accounts.structs.rest.UpdateAccountListPasswordBody;
import com.iambadatplaying.server.rest.RestContextHandler;
import com.iambadatplaying.server.rest.servlets.ServletUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

@Path("/")
public class AccountServlet implements BasicServlet {

    @Context
    private ServletContext context;

    private Gson gson = new Gson();

    private Optional<AccountModule> getAccountModule() {
        Starter starter = Util.castUnsafe(context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER));

        if (starter == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(Util.castUnsafe(starter.getBasicModuleLoader().getModule(AccountModule.class)));
    }

    private Optional<ValidatingAccessMap<String, AccountList>> getAccountListMap() {
        return getAccountModule().map(AccountModule::getAccountMaps);
    }

    private Optional<AccountList> getAccountList(String listId) {
        return getAccountListMap().map(map -> map.get(listId));
    }

    private Optional<Account> getAccount(String listId, String accountId) {
        return getAccountList(listId).map(accountList -> accountList.getAccount(accountId));
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() {
        Optional<AccountModule> optAccountModule = getAccountModule();
        if (!optAccountModule.isPresent()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        AccountModule accountModule = optAccountModule.get();

        Optional<BasicConfig> config = accountModule.getConfiguration();
        if (!config.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(config.get().getJson())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @GET
    @Path("/lists")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountLists() {
        Optional<AccountModule> optAccountModule = getAccountModule();
        if (!optAccountModule.isPresent()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        AccountModule accountModule = optAccountModule.get();

        Map<String, AccountList> accountMapMap = accountModule.getAccountMaps();
        if (accountMapMap == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(gson.toJson(accountMapMap))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @POST
    @Path("/lists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addAccountList(JsonElement data) {
        AddAccountListBody addAccountListBody = gson.fromJson(data, AddAccountListBody.class);
        if (!addAccountListBody.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();

        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();

        AccountList accountList = new AccountList(addAccountListBody.getName(), addAccountListBody.getMasterPassword());

        try {
            accountListMap.put(accountList.getId(), accountList);
        } catch (InvalidAccessException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ServletUtils.createResponseJson("Invalid Access", e.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.CREATED).entity(ServletUtils.createResponseJson("Created List", accountList.getId())).build();
    }

    @GET
    @Path("/lists/{listId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountListById(@PathParam("listId") String listId) {
        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();

        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();

        AccountList accountList = accountListMap.get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(gson.toJson(accountList))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @PUT
    @Path("/lists/{listId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccountList(@PathParam("listId") String listId, JsonElement data) {
        UpdateAccountListBody updateAccountListBody = gson.fromJson(data, UpdateAccountListBody.class);
        if (!updateAccountListBody.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();

        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();
        AccountList accountList = accountListMap.get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        accountList.setName(updateAccountListBody.getName());

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @DELETE
    @Path("/lists/{listId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccountList(@PathParam("listId") String listId) {
        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();

        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();

        try {
            accountListMap.remove(listId);
        } catch (InvalidAccessException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ServletUtils.createResponseJson("Invalid Access", e.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/lists/{listId}/unlock")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unlockAccountList(@PathParam("listId") String listId, JsonElement data) {
        if (data == null || !data.isJsonObject()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String password = data.getAsJsonObject().get("password").getAsString();

        Optional<AccountModule> optAccountModule = getAccountModule();
        if (!optAccountModule.isPresent()) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        AccountModule accountModule = optAccountModule.get();

        AccountList accountList = accountModule.getAccountMaps().get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!accountList.unlock(password)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @PUT
    @Path("/lists/{listId}/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccountListPassword(@PathParam("listId") String listId, JsonElement data) {
        UpdateAccountListPasswordBody updateAccountListPasswordBody = gson.fromJson(data, UpdateAccountListPasswordBody.class);
        if (!updateAccountListPasswordBody.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();
        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();

        AccountList accountList = accountListMap.get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        if (!accountList.changeMasterPassword(updateAccountListPasswordBody.getNewPassword())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ServletUtils.createResponseJson("Invalid Access", "Invalid password provided"))
                    .build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/lists/{listId}/lock")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lockAccountList(@PathParam("listId") String listId) {
        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();
        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();
        AccountList accountList = accountListMap.get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        if (!accountList.lock()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/lists/{listId}/accounts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccounts(@PathParam("listId") String listId) {
        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();
        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();
        AccountList accountList = accountListMap.get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(gson.toJson(accountList.getAccounts()))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @POST
    @Path("/lists/{listId}/accounts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addAccount(@PathParam("listId") String listId, JsonElement data) {
        AddAccountBody addAccountBody = gson.fromJson(data, AddAccountBody.class);
        if (!addAccountBody.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();
        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();
        AccountList accountList = accountListMap.get(listId);

        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        Account account = new Account(addAccountBody.getLoginName(), addAccountBody.getLoginPassword());

        if (!accountList.addAccount(account)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ServletUtils.createResponseJson("Invalid Access", "Not allowed to add account"))
                    .build();
        }

        return Response.status(Response.Status.CREATED)
                 .entity(ServletUtils.createResponseJson("Created Account", account.getUuid()))
                .build();
    }

    @GET
    @Path("/lists/{listId}/accounts/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountById(@PathParam("listId") String listId, @PathParam("accountId") String accountId) {

        Optional<ValidatingAccessMap<String, AccountList>> optAccountListMap = getAccountListMap();
        if (!optAccountListMap.isPresent()) {
            return ServletUtils.createStandardProcessingErrorResponse();
        }

        ValidatingAccessMap<String, AccountList> accountListMap = optAccountListMap.get();
        AccountList accountList = accountListMap.get(listId);
        if (accountList == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        Account account = accountList.getAccount(accountId);
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("Account not found"))
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(account.serializeSensitive())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    @PUT
    @Path("/lists/{listId}/accounts/{accountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccount(@PathParam("listId") String listId, @PathParam("accountId") String accountId, JsonElement data) {
        return null;
    }

    @DELETE
    @Path("/lists/{listId}/accounts/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccount(@PathParam("listId") String listId, @PathParam("accountId") String accountId) {
        Optional<AccountList> optAccountList = getAccountList(listId);
        if (!optAccountList.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ServletUtils.createResponseJson("List not found"))
                    .build();
        }

        AccountList accountList = optAccountList.get();

        if (!accountList.removeAccount(accountId)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ServletUtils.createResponseJson("Invalid Access", "Not allowed to remove account"))
                    .build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/lists/{listId}/accounts/{accountId}/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(JsonElement data) {
        Starter starter = (Starter) context.getAttribute(RestContextHandler.KEY_CONTEXT_STARTER);
        if (starter == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        RSOAuthenticationManager mgr = Util.castUnsafe(starter.getDataManger().getObjectDataManager(RSOAuthenticationManager.class));
        if (mgr == null) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        return null;
    }
}
