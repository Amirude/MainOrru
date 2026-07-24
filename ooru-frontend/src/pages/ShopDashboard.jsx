import { useEffect, useState } from "react";
import { apiClient, extractErrorMessage } from "../api/client";
import { findCategory, CART_CATEGORIES } from "../categories";

const CART_BASED_CODES = new Set(
  CART_CATEGORIES.map((c) => c.code)
);

export default function ShopDashboard() {
  const [shops, setShops] = useState([]);
  const [selectedShopId, setSelectedShopId] = useState(null);

  const [bookings, setBookings] = useState([]);
  const [menu, setMenu] = useState([]);
  const [slots, setSlots] = useState([]);

  const [error, setError] = useState("");

  const [showRegisterForm, setShowRegisterForm] = useState(false);

  const [categoryOptions, setCategoryOptions] = useState([]);

  const [menuForm, setMenuForm] = useState({
    name: "",
    priceRupees: "",
    imageUrl: ""
  });

  const [slotForm, setSlotForm] = useState({
    date: "",
    startTime: "",
    endTime: ""
  });

  const [billing, setBilling] = useState({
    gstPercent: "",
    handlingFeeRupees: ""
  });

  const [form, setForm] = useState({
    shopName: "",
    categoryCode: "",
    address: ""
  });


  async function loadShops() {
    try {
      const res = await apiClient.get("/shops/mine");

      setShops(res.data);

      if (res.data.length > 0 && !selectedShopId) {
        setSelectedShopId(res.data[0].id);
      }

    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }


  async function loadBookings(shopId) {

    if (!shopId) return;

    try {

      const res = await apiClient.get(
        `/bookings/shop/${shopId}`
      );

      setBookings(res.data);

    } catch(err){
      setError(extractErrorMessage(err));
    }

  }


  async function loadMenu(shopId){

    if(!shopId){
      setMenu([]);
      return;
    }

    try{

      const res = await apiClient.get(
        `/shops/${shopId}/menu`
      );

      setMenu(res.data);

    }catch(err){
      setError(extractErrorMessage(err));
    }

  }



  async function loadSlots(shopId){

    if(!shopId){
      setSlots([]);
      return;
    }

    try{

      const res = await apiClient.get(
        `/shops/${shopId}/slots`
      );

      setSlots(res.data);

    }catch(err){
      setError(extractErrorMessage(err));
    }

  }



  useEffect(()=>{

    loadShops();


    apiClient.get("/categories")
    .then(res=>{

      setCategoryOptions(res.data);


      if(res.data.length>0){

        setForm(f=>({
          ...f,
          categoryCode:res.data[0].code
        }));

      }

    })
    .catch(err=>{
      setError(extractErrorMessage(err));
    });


  },[]);



  useEffect(()=>{

    loadBookings(selectedShopId);
    loadMenu(selectedShopId);
    loadSlots(selectedShopId);

  },[selectedShopId]);





  async function handleRegisterShop(e){

    e.preventDefault();

    try{

      await apiClient.post(
        "/shops/register",
        form
      );


      setShowRegisterForm(false);


      setForm({
        shopName:"",
        categoryCode:"",
        address:""
      });


      loadShops();


    }catch(err){

      setError(extractErrorMessage(err));

    }

  }





  async function handleAddMenuItem(e){

    e.preventDefault();


    try{

      const pricePaise =
        Math.round(
          parseFloat(menuForm.priceRupees) * 100
        );


      await apiClient.post(
        `/shops/${selectedShopId}/menu`,
        {
          name:menuForm.name,
          pricePaise,
          imageUrl:menuForm.imageUrl || null
        }
      );


      setMenuForm({
        name:"",
        priceRupees:"",
        imageUrl:""
      });


      loadMenu(selectedShopId);


    }catch(err){

      setError(extractErrorMessage(err));

    }

  }





  async function updateStatus(id,status){

    try{

      await apiClient.patch(
        `/bookings/${id}/status`,
        {
          status
        }
      );


      loadBookings(selectedShopId);


    }catch(err){

      setError(extractErrorMessage(err));

    }

  }





  const selectedShop =
    shops.find(
      s=>s.id===selectedShopId
    );



  return (

    <div className="wrap">


      <div className="card">

        <h2>
          Shop Dashboard
        </h2>


        {error &&
          <div className="error-banner">
            {error}
          </div>
        }


        {shops.length===0 &&

          <button
          className="btn"
          onClick={()=>setShowRegisterForm(true)}
          >
            Register Shop
          </button>

        }



        {showRegisterForm &&

        <form onSubmit={handleRegisterShop}>


          <input
          placeholder="Shop name"
          value={form.shopName}
          onChange={
            e=>setForm({
              ...form,
              shopName:e.target.value
            })
          }
          />


          <select
          value={form.categoryCode}
          onChange={
            e=>setForm({
              ...form,
              categoryCode:e.target.value
            })
          }
          >

          {
            categoryOptions.map(c=>

              <option
              key={c.code}
              value={c.code}
              >

              {c.displayName}

              </option>

            )
          }

          </select>



          <input
          placeholder="Address"
          value={form.address}
          onChange={
            e=>setForm({
              ...form,
              address:e.target.value
            })
          }
          />


          <button className="btn">
            Submit
          </button>


        </form>

        }


      </div>





      {selectedShop &&

      <div className="card">

      <h2>
      Bookings
      </h2>


      {
      bookings.map(b=>{

        const cat=findCategory(
          b.categoryCode
        );


        return (

        <div
        className="booking-row"
        key={b.id}
        >


        <div>
        <b>{b.reference}</b>
        {" - "}
        {cat ? cat.name : b.categoryCode}
        </div>



        <span>
        {b.status}
        </span>



        {
        b.status==="REQUESTED" &&

        <>
        <button
        className="btn"
        onClick={()=>
        updateStatus(
          b.id,
          "ACCEPTED"
        )}
        >
        Accept
        </button>


        <button
        className="btn brick"
        onClick={()=>
        updateStatus(
          b.id,
          "REJECTED"
        )}
        >
        Reject
        </button>
        </>

        }


        </div>

        )

      })
      }


      </div>

      }



    </div>

  );

}