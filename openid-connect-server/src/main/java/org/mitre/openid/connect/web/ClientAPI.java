/*******************************************************************************
 * Copyright 2012 The MITRE Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mitre.openid.connect.web;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.mitre.oauth2.exception.ClientNotFoundException;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Collection;
import java.util.UUID;

/**
 * @author Michael Jett <mjett@mitre.org>
 */

@Controller
@RequestMapping("/api/clients")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class ClientAPI {

    @Autowired
    private ClientDetailsEntityService clientService;

    /**
     * constructor
     */
    public ClientAPI() {

    }

    /**
     * Get a list of all clients
     * @param modelAndView
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, headers="Accept=application/json")
    public ModelAndView apiGetAllClients(ModelAndView modelAndView) {

        Collection<ClientDetailsEntity> clients = clientService.getAllClients();
        modelAndView.addObject("entity", clients);
        modelAndView.setViewName("jsonClientView");

        return modelAndView;
    }

    /**
     * Create a new client
     * @param json
     * @param m
     * @param principal
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, headers = "Accept=application/json")
    public String apiAddClient(@RequestBody String jsonString, Model m, Principal principal) {

    	// TODO: sanity check if the thing really is a JSON object
    	JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();

    	// parse the client passed in (from JSON) and fetch the old client from the store
        ClientDetailsEntity client = new Gson().fromJson(json, ClientDetailsEntity.class);
        
        // if they leave the client secret empty, force it to be generated
        if (Strings.isNullOrEmpty(client.getClientId())) {
        	client = clientService.generateClientId(client);
        }
        
        // if they've asked for us to generate a client secret, do so here
        if (json.has("generateClientSecret") && json.get("generateClientSecret").getAsBoolean()) {
        	client = clientService.generateClientSecret(client);
        }
        
        // set owners as current logged in user
        //client.setOwner(principal.getName());
        //TODO: owner has been replaced by a list of contacts, which should be styled as email addresses.
        client.setDynamicallyRegistered(false);
        
        m.addAttribute("entity", clientService.saveNewClient(client));

        return "jsonClientView";
    }

    /**
     * Update an existing client
     * @param id
     * @param jsonString
     * @param m
     * @param principal
     * @return
     */
    @RequestMapping(value="/{id}", method = RequestMethod.PUT, headers = "Accept=application/json")
    public String apiUpdateClient(@PathVariable("id") Long id, @RequestBody String jsonString, Model m, Principal principal) {
    	
    	// TODO: sanity check if the thing really is a JSON object
    	JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();

    	// parse the client passed in (from JSON) and fetch the old client from the store
        ClientDetailsEntity client = new Gson().fromJson(json, ClientDetailsEntity.class);
        ClientDetailsEntity oldClient = clientService.getClientById(id);
        
        if (oldClient == null) {
        	throw new ClientNotFoundException();
        }
        
        // if they leave the client secret empty, force it to be generated
        if (Strings.isNullOrEmpty(client.getClientId())) {
        	client = clientService.generateClientId(client);
        }
        
        // if they've asked for us to generate a client secret, do so here
        if (json.has("generateClientSecret") && json.get("generateClientSecret").getAsBoolean()) {
        	client = clientService.generateClientSecret(client);
        }
        
        // set owners as current logged in user
        // client.setOwner(principal.getName());
        //TODO: owner has been replaced by a list of contacts, which should be styled as email addresses.
        
        m.addAttribute("entity", clientService.updateClient(oldClient, client));

        return "jsonClientView";
    }

    /**
     * Delete a client
     * @param id
     * @param modelAndView
     * @return
     */
    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, headers="Accept=application/json")
    public String apiDeleteClient(@PathVariable("id") Long id, ModelAndView modelAndView) {

        ClientDetailsEntity client = clientService.getClientById(id);
        clientService.deleteClient(client);

        return "jsonClientView";
    }


    /**
     * Get an individual client
     * @param id
     * @param modelAndView
     * @return
     */
    @RequestMapping(value="/{id}", method=RequestMethod.GET, headers="Accept=application/json")
    @ResponseBody
    public Object apiShowClient(@PathVariable("id") Long id, ModelAndView modelAndView) {
        ClientDetailsEntity client = clientService.getClientById(id);
        if (client == null) {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }

        modelAndView.addObject("entity", client);
        modelAndView.setViewName("jsonClientView");

        return modelAndView;
    }
}
