package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.households.HouseholdsSchemaV10Names;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Stack;

class VehicleReaderV2 extends MatsimXmlParser{
	private static final Logger log = Logger.getLogger( VehicleReaderV2.class ) ;

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;
	private VehicleCapacity currentCapacity = null;
	private FreightCapacity currentFreightCapacity = null;
	private EngineInformation.FuelType currentFuelType = null;
	private double currentGasConsumption = Double.NaN;

	private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes =
		  new org.matsim.utils.objectattributes.attributable.Attributes();

	public VehicleReaderV2( final Vehicles vehicles ){
		log.info("Using " + this.getClass().getName());
		this.vehicles = vehicles;
		this.builder = this.vehicles.getFactory();
	}

	@Override
	public void endTag( final String name, final String content, final Stack<String> context ){
		if( VehicleSchemaV1Names.DESCRIPTION.equalsIgnoreCase( name ) && (content.trim().length() > 0) ){
			this.currentVehType.setDescription( content.trim() );
		} else if( VehicleSchemaV1Names.ENGINEINFORMATION.equalsIgnoreCase( name ) ){
			EngineInformation currentEngineInfo = this.builder.createEngineInformation( this.currentFuelType, this.currentGasConsumption );
			this.currentVehType.setEngineInformation( currentEngineInfo );
			this.currentFuelType = null;
			this.currentGasConsumption = Double.NaN;
		} else if( VehicleSchemaV1Names.FUELTYPE.equalsIgnoreCase( name ) ){
			this.currentFuelType = this.parseFuelType( content.trim() );
		} else if( VehicleSchemaV1Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
			this.currentCapacity.setFreightCapacity( this.currentFreightCapacity);
			this.currentFreightCapacity = null;
		} else if( VehicleSchemaV1Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentVehType.setCapacity( this.currentCapacity );
			this.currentCapacity = null;
		} else if( VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.vehicles.addVehicleType( this.currentVehType );
			this.currentVehType = null;
		} else if (name.equalsIgnoreCase( HouseholdsSchemaV10Names.ATTRIBUTES )) {
			this.currAttributes = null;
		}
		else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTE)) {
			this.attributesReader.endTag( name , content , context );
		}

	}

	private EngineInformation.FuelType parseFuelType( final String content ){
		if( EngineInformation.FuelType.gasoline.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.gasoline;
		} else if( EngineInformation.FuelType.diesel.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.diesel;
		} else if( EngineInformation.FuelType.electricity.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.electricity;
		} else if( EngineInformation.FuelType.biodiesel.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.biodiesel;
		} else{
			throw new IllegalArgumentException( "Fuel type: " + content + " is not supported!" );
		}
	}

	private VehicleType.DoorOperationMode parseDoorOperationMode( final String modeString ){
		if( VehicleType.DoorOperationMode.serial.toString().equalsIgnoreCase( modeString ) ){
			return VehicleType.DoorOperationMode.serial;
		} else if( VehicleType.DoorOperationMode.parallel.toString().equalsIgnoreCase( modeString ) ){
			return VehicleType.DoorOperationMode.parallel;
		} else{
			throw new IllegalArgumentException( "Door operation mode " + modeString + " is not supported" );
		}
	}

	@Override
	public void startTag( final String name, final Attributes atts, final Stack<String> context ){
		if( VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.currentVehType = this.builder.createVehicleType( Id.create( atts.getValue( VehicleSchemaV1Names.ID ), VehicleType.class ) );
		} else if( VehicleSchemaV1Names.LENGTH.equalsIgnoreCase( name ) ){
			this.currentVehType.setLength( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.METER ) ) );
		} else if( VehicleSchemaV1Names.WIDTH.equalsIgnoreCase( name ) ){
			this.currentVehType.setWidth( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.METER ) ) );
		} else if( VehicleSchemaV1Names.MAXIMUMVELOCITY.equalsIgnoreCase( name ) ){
			double val = Double.parseDouble( atts.getValue( VehicleSchemaV1Names.METERPERSECOND ) );
			if( val == 1.0 ){
				log.warn(
					  "The vehicle type's maximum velocity is set to 1.0 meter per second, is this really intended? vehicletype = " + this.currentVehType.getId().toString() );
			}
			this.currentVehType.setMaximumVelocity( val );
		} else if( VehicleSchemaV1Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentCapacity = this.builder.createVehicleCapacity();
		} else if( VehicleSchemaV1Names.SEATS.equalsIgnoreCase( name ) ){
			this.currentCapacity.setSeats( Integer.valueOf( atts.getValue( VehicleSchemaV1Names.PERSONS ) ) );
		} else if( VehicleSchemaV1Names.STANDINGROOM.equalsIgnoreCase( name ) ){
			this.currentCapacity.setStandingRoom( Integer.valueOf( atts.getValue( VehicleSchemaV1Names.PERSONS ) ) );
		} else if( VehicleSchemaV1Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity = this.builder.createFreigthCapacity();
		} else if( VehicleSchemaV1Names.VOLUME.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity.setVolume( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.CUBICMETERS ) ) );
		} else if( VehicleSchemaV1Names.WEIGHT.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity.setWeight( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.TONS) ) );
		} else if( VehicleSchemaV1Names.UNIT.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity.setUnits( Integer.parseInt( atts.getValue( VehicleSchemaV1Names.UNITS ) ) );
		} else if( VehicleSchemaV1Names.GASCONSUMPTION.equalsIgnoreCase( name ) ){
			this.currentGasConsumption = Double.parseDouble( atts.getValue( VehicleSchemaV1Names.LITERPERMETER ) );
		} else if( VehicleSchemaV1Names.VEHICLE.equalsIgnoreCase( name ) ){
			Id<VehicleType> typeId = Id.create( atts.getValue( VehicleSchemaV1Names.TYPE ), VehicleType.class );
			VehicleType type = this.vehicles.getVehicleTypes().get( typeId );
			if( type == null ){
				log.error( "VehicleType " + typeId + " does not exist." );
			}
			String idString = atts.getValue( VehicleSchemaV1Names.ID );
			Id<Vehicle> id = Id.create( idString, Vehicle.class );
			Vehicle v = this.builder.createVehicle( id, type );
			this.vehicles.addVehicle( v );
		} else if( VehicleSchemaV1Names.ACCESSTIME.equalsIgnoreCase( name ) ){
			this.currentVehType.setAccessTime( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.SECONDSPERPERSON ) ) );
		} else if( VehicleSchemaV1Names.EGRESSTIME.equalsIgnoreCase( name ) ){
			this.currentVehType.setEgressTime( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.SECONDSPERPERSON ) ) );
		} else if( VehicleSchemaV1Names.DOOROPERATION.equalsIgnoreCase( name ) ){
			this.currentVehType.setDoorOperationMode( this.parseDoorOperationMode( atts.getValue( VehicleSchemaV1Names.MODE ) ) );
		} else if( VehicleSchemaV1Names.PASSENGERCAREQUIVALENTS.equalsIgnoreCase( name ) ){
			this.currentVehType.setPcuEquivalents( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.PCE ) ) );
		} else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTES)) {
//			if (context.peek().equalsIgnoreCase(HouseholdsSchemaV10Names.HOUSEHOLD)) {
			if (context.peek().equalsIgnoreCase(VehicleSchemaV1Names.VEHICLETYPE)) {
				currAttributes = this.currentVehType.getAttributes();
				attributesReader.startTag( name , atts , context, currAttributes );
			}
		}
		else if (name.equalsIgnoreCase(HouseholdsSchemaV10Names.ATTRIBUTE)) {
			attributesReader.startTag( name , atts , context, currAttributes );
		}

	}

}